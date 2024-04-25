/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
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

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.SystemCall;
import js.file.FileException;
import js.file.Files;
import js.webtools.RemoteManager;

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
 * 
 * Things to investigate
 * =====================
 * Rsync has a clunky way of distinguishing between copying a directory vs copying its contents.
 * If the source directory has a trailing slash, it interprets this as copying the contents; otherwise,
 * it copies the directory.  
 * 
 * For our purposes, we usually want to copy a directory, and without changing its name; so we probably
 * want to NOT add a trailing slash to the source, and also trim the last directory's name from the target path.
 * 
 * ACTUALLY, now I think the trailing slash is a good way to get it to send just the contents...
 * 
 * 
 * 
 * It is also tricky because this behaviour only applies to directories, and we don't know ahead of time
 * (if we're pulling in something from a remote machine) whether we're talking about a directory or a file.
 * If we're pushing, then we know if it's a directory; if we're pulling, we could see if a file already exists
 * on the local machine, and see if it is a directory or not.  Failing that, we could guess if it's a directory
 * based upon whether it has an extension (and probably generate all sorts of errors if we try to push/pull 
 * directories that have extensions).
 * 
 * We may want to disallow renaming a directory (or file) that is being pulled... e.g., allow it to be 
 * placed in a different directory, but ensure that the last path element matches.
 * 
 * At present, if absolute file paths are given, it complains about missing other paths.  We could check if the
 * absolute path lies within the project, and if so, assume the missing path should be the symmetric counterpart.
 * 
 * </pre>
 */
public abstract class RsyncOper extends AppOper {

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

    mSourceContentsOnly = arg0.endsWith("/");
    mSourceEntityPath = new File(arg0);
    pr("arg0:", quote(arg0), CR, "sourcePath:", mSourceEntityPath, CR, "contents only:", mSourceContentsOnly);

    if (arg1 != null)
      mTargetEntityPath = new File(arg1);

    args.assertArgsDone();
  }

  @Override
  public void perform() {
    determinePaths();

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

    // Create directories on remote machine, in case it doesn't exist; see https://stackoverflow.com/questions/1636889
    s.arg("--mkpath");

    if (!isPull()) {
      {
        var a = mResolvedSource.toString();
        if (mSourceContentsOnly)
          a += "/";
        s.arg(a);
      }
      addRemoteArg(s, mResolvedTarget);
    } else {
      addRemoteArg(s, mResolvedSource);
      s.arg(mResolvedTarget);
    }

    s.assertSuccess();
  }

  private void addRemoteArg(SystemCall s, File resolvedPath) {
    var mgr = RemoteManager.SHARED_INSTANCE;
    var entInfo = mgr.activeEntity();

    StringBuilder sb = new StringBuilder();

    if (entInfo.port() != 0) {
      // I don't think we need to add quotes around the (single) argument [ssh -p]; in fact,
      // it maybe causes the SystemCall to fail 
      s.arg("-e", "ssh -p" + entInfo.port());
    }

    sb.append(checkNonEmpty(entInfo.user()));
    sb.append('@');
    sb.append(checkNonEmpty(entInfo.url()));
    sb.append(':');
    sb.append(resolvedPath);
    s.arg(sb);
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
    log("determinePushPaths");

    File sourceRelativeToProject = null;
    {
      // If no source directory was given, assume the current directory
      //
      File sourceDir = mSourceEntityPath;
      log("mSourceEntityPath:", mSourceEntityPath);

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
        log("source wasn't absolute, local project dir:", sourceBaseDir);
      }
      log("mResolvedSource:", mResolvedSource);
    }

    {
      // If no target directory was given, assume it has the same relative path as the source directory
      File targetDir = mTargetEntityPath;
      log("mTargetEntityPath:", mTargetEntityPath);
      if (Files.empty(targetDir)) {
        if (sourceRelativeToProject == null)
          throw badArg("Absolute source directory given with no target directory");

        // Omit the final component (xxx/yyy[/foo]) otherwise if foo is a directory,
        // we will end up with xxx/yyy/foo/foo on the target.

        File truncatedSource = Files.parent(sourceRelativeToProject);
        log("remoteProjectDir:", remoteProjectDir());
        mResolvedTarget = Files.join(remoteProjectDir(), truncatedSource);
      } else {
        if (targetDir.isAbsolute())
          mResolvedTarget = targetDir;
        else {
          log("target is relative, joining with remote project dir", remoteProjectDir());
          mResolvedTarget = Files.join(remoteProjectDir(), targetDir);
        }
        // TODO "Issue #32: haven't yet dealt with the copy directory contents vs copy directory (i.e. by adding '/')");
      }
      log("mResolvedTarget:", mResolvedTarget);
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
        // If pulling an immediate subdirectory of the project dir, it's a special case
        if (sourceRelativeToProject.getParent() == null) {
          mResolvedTarget = localBaseDir;
        } else {
          File truncatedSource = Files.parent(sourceRelativeToProject);
          mResolvedTarget = Files.join(localBaseDir, truncatedSource);
        }
      } else {
        if (targetDir.isAbsolute())
          mResolvedTarget = targetDir;
        else
          mResolvedTarget = Files.join(localProjectDir(), targetDir);
        todo(
            "Issue #32: haven't yet dealt with the copy directory contents vs copy directory (i.e. by adding '/')");
      }
      log("mResolvedTarget:", mResolvedTarget);
    }
  }

  private List<String> excludeExpressionList() {
    if (mCachedExcludeExpressionsList == null) {
      List<String> strings = arrayList();
      mCachedExcludeExpressionsList = strings;
      String script = frag("rsync_excludes.txt");
      for (String s : split(script, '\n')) {
        s = s.trim();
        if (s.isEmpty() || s.startsWith("#"))
          continue;
        strings.add(s);
      }
    }
    return mCachedExcludeExpressionsList;
  }

  private String frag(String resourceName) {
    String s = null;
    try {
      s = Files.readString(getClass(), "rsync/" + resourceName);
    } catch (FileException e) {
      var f = new File("/Users/home/github_projects/dev/src/main/resources/dev/rsync/" + resourceName);
      if (!f.exists())
        throw e;
      s = Files.readString(f);
    }
    return s;
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

  private File localProjectDir() {
    if (mCachedLocalProjectDir == null)
      mCachedLocalProjectDir = Files
          .parent(Files.getFileWithinParents(null, ".git", "git repository containing current directory"));
    return mCachedLocalProjectDir;
  }

  private File remoteProjectDir() {
    if (mCachedRemoteProjectDir == null) {
      var mgr = RemoteManager.SHARED_INSTANCE;
      var ent = mgr.activeEntity();
      var f = ent.projectDir();
      if (Files.empty(f)) {
        alert("setting empty project_dir to /root");
        f = new File("/root");
      }
      mCachedRemoteProjectDir = Files.assertAbsolute(f);
    }
    return mCachedRemoteProjectDir;
  }

  protected File mSourceEntityPath = Files.DEFAULT;
  protected File mTargetEntityPath = Files.DEFAULT;
  private File mResolvedSource;
  private File mResolvedTarget;
  private List<String> mCachedExcludeExpressionsList;
  private File mCachedLocalProjectDir;
  private File mCachedRemoteProjectDir;
  private boolean mSourceContentsOnly;
}
