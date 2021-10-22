/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.List;

import dev.gen.RemoteEntityInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.SystemCall;
import js.file.Files;

/**
 * <pre>
 * 
 * An rsync wrapper to simplify moving files between local and remote machines.
 * 
 * Assumptions that simplify things:
 * 
 * [] The remote machine is declared beforehand (see EntityOper), so details 
 *      such as its url, port, home directory, etc are abstracted away
 *      
 * [] Paths are generally assumed to lie within the respective machine's project 
 *      directories (on the local machine, this is taken to be the current directory's
 *      containing git repository)
 *      
 * [] An extensive list of files to omit is provided, so they (in many cases) don't need to be 
 *      provided by the user
 * 
 * </pre>
 */
public abstract class RsyncOper extends AppOper {

  private static final boolean EXPERIMENT = false && alert("performing experiment");

  /**
   * The 'pull' operation should override this method to return true
   */
  protected boolean isPull() {
    return false;
  }

  @Override
  protected final void processAdditionalArgs() {

    CmdLineArgs args = app().cmdLineArgs();
    String arg0 = args.nextArg();
    String arg1 = null;
    if (args.hasNextArg())
      arg1 = args.nextArg();

    if (EXPERIMENT) {
      arg0 = "jv_exp";
      arg1 = ""; //"rel/target/foo/bar";
    }

    mSourceEntityPath = new File(arg0);
    if (arg1 != null)
      mTargetEntityPath = new File(arg1);

    args.assertArgsDone();
  }

  @Override
  public void perform() {

    determinePaths();
    if (EXPERIMENT)
      halt();

    SystemCall s = new SystemCall();
    boolean verbosity = verbose();
    s.withVerbose(verbosity);
    s.arg("rsync");

    // Ok, I figured out why it seems to be transferring a lot of files unnecessarily.
    // The remote machine's files have different timestamps, due to them being installed via git.
    // This can be avoided by using options other than --archive; but it will slow things down,
    // so I'm going to stick with --archive:

    s.arg("--archive");

    if (verbosity)
      s.arg("--verbose");
    if (dryRun())
      s.arg("--dry-run");

    s.arg("--exclude-from=" + constructExcludeList());

    addMachineArg(s, mResolvedSource, isPull());
    addMachineArg(s, mResolvedTarget, !isPull());

    s.assertSuccess();
  }

  private void addMachineArg(SystemCall s, File resolvedPath, boolean isRemote) {
    if (!isRemote)
      s.arg(resolvedPath);
    else {
      RemoteEntityInfo ent = remoteEntity();
      checkArgument(ent.port() > 0, "bad port:", INDENT, ent);

      s.arg("-e", "ssh -p" + ent.port());

      {
        StringBuilder sb = new StringBuilder();

        checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
        checkArgument(nonEmpty(ent.url()), "no url:", INDENT, ent);
        sb.append(ent.user());
        sb.append('@');
        sb.append(ent.url());
        sb.append(':');

        sb.append(resolvedPath);
        s.arg(sb);
      }
    }
  }

  /**
   * Determine the source and target paths involved, and detect appropriate
   * problems
   */
  private void determinePaths() {
    if (!isPull())
      determinePushPaths();
    else
      determinePullPaths();
    if (verbose()) {
      pr("source arg:", mSourceEntityPath);
      pr("target arg:", mTargetEntityPath);
      pr("source resolved:", mResolvedSource);
      pr("target resolved:", mResolvedTarget);
    }
  }

  private void determinePushPaths() {

    File sourceRelativeToProject = null;
    {
      // If no source directory was given, assume the current directory
      //
      File sourceDir = mSourceEntityPath;

      if (Files.empty(sourceDir)) {
        sourceDir = new File(".");
        if (alert("verifying"))
          checkState(!sourceDir.isAbsolute());
      }

      if (sourceDir.isAbsolute()) {
        mResolvedSource = sourceDir;
      } else {
        File sourceBaseDir = localProjectDir();
        sourceRelativeToProject = Files.relativeToContainingDirectory(sourceDir, sourceBaseDir);
        mResolvedSource = Files.join(sourceBaseDir, sourceRelativeToProject);
      }
    }

    {
      // If no target directory was given, assume it has the same relative path as the source directory
      File targetDir = mTargetEntityPath;
      if (Files.empty(targetDir)) {
        if (sourceRelativeToProject == null)
          throw badArg("Absolute source directory given with no target directory");
        mResolvedTarget = Files.join(remoteProjectDir(), sourceRelativeToProject);
      } else {
        if (targetDir.isAbsolute())
          mResolvedTarget = targetDir;
        else
          mResolvedTarget = Files.join(remoteProjectDir(), targetDir);
      }
    }
  }

  private void determinePullPaths() {

    File sourceRelativeToProject = null;
    {
      if (Files.empty(mSourceEntityPath))
        throw badArg("No remote path given");
      if (mSourceEntityPath.isAbsolute()) {
        mResolvedSource = mSourceEntityPath;
      } else {
        sourceRelativeToProject = mSourceEntityPath;
        mResolvedSource = Files.join(remoteProjectDir(), sourceRelativeToProject);
      }
    }

    {
      // If no target directory was given, assume it has the same relative path as the source directory
      File targetDir = mTargetEntityPath;
      if (Files.empty(targetDir)) {
        if (sourceRelativeToProject == null)
          throw badArg("Absolute source directory given with no target directory");
        File localBaseDir = localProjectDir();
        mResolvedTarget = Files.join(localBaseDir, sourceRelativeToProject);
      } else {
        if (targetDir.isAbsolute())
          mResolvedTarget = targetDir;
        else
          mResolvedTarget = Files.join(localProjectDir(), targetDir);
      }
    }
  }

  private List<String> excludeExpressionList() {
    if (mCachedExcludeExpressionsList == null) {
      List<String> strings = arrayList();
      mCachedExcludeExpressionsList = strings;
      String script = Files.readString(getClass(), "rsync_excludes.txt");
      for (String s : split(script, '\n')) {
        s = s.trim();
        if (s.isEmpty() || s.startsWith("#"))
          continue;
        strings.add(s);
      }
    }
    return mCachedExcludeExpressionsList;
  }

  /**
   * Generate a file containing an exclude list to be passed to rsync
   */
  private File constructExcludeList() {
    File tempFile;
    if (verbose())
      tempFile = new File(Files.homeDirectory(), "_SKIP_RsyncOper_excludes.txt");
    else {
      tempFile = Files.createTempFile("RsyncExcludes", ".txt");
      tempFile.deleteOnExit();
    }

    StringBuilder sb = new StringBuilder();
    for (String expr : excludeExpressionList()) {
      sb.append("- ");
      sb.append(expr);
      sb.append('\n');
    }

    Files.S.writeString(tempFile, sb.toString());
    return tempFile;
  }

  private RemoteEntityInfo remoteEntity() {
    return EntityManager.sharedInstance().activeEntity();
  }

  private File localProjectDir() {
    if (mCachedLocalProjectDir == null)
      mCachedLocalProjectDir = Files.parent(Files.getFileWithinParents(Files.currentDirectory(), ".git",
          "git repository containing current directory"));
    return mCachedLocalProjectDir;
  }

  private File remoteProjectDir() {
    if (mCachedRemoteProjectDir == null)
      mCachedRemoteProjectDir = Files.assertAbsolute(remoteEntity().projectDir());
    return mCachedRemoteProjectDir;
  }

  protected File mSourceEntityPath = Files.DEFAULT;
  protected File mTargetEntityPath = Files.DEFAULT;
  private File mResolvedSource;
  private File mResolvedTarget;
  private List<String> mCachedExcludeExpressionsList;
  private File mCachedLocalProjectDir;
  private File mCachedRemoteProjectDir;
}
