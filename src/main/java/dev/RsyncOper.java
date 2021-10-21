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

    if (alert("using debug args")) {
      arg0 = "jv_exp";
      arg1 = "foo/bar";
    }

    mSourceEntityPath = parseEntityPath(arg0);
    if (arg1 != null)
      mTargetEntityPath = parseEntityPath(arg1);
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

    s.arg(Files.join(mSourceBaseDir, mSourceRelativeToProject));

    RemoteEntityInfo ent = EntityManager.sharedInstance().activeEntity();
    checkArgument(ent.port() > 0, "bad port:", INDENT, ent);

    s.arg("-e", "ssh -p" + ent.port());

    // Determine the target directory

    File target = Files.join(mTargetBaseDir, mTargetRelativeToProject);
    //   checkArgument(sourceDirString.startsWith(sourceBaseDirStr));
    //      String sourceOffsetString = sourceDirString.substring(sourceBaseDirStr.length());
    //      targetDirString = targetBaseDir() + sourceOffsetString;

    // Omit the target directory name, so rsync will create one with the same name as the source.
    //String targetDirString = Files.parent(target).toString();

    // Omit the target directory name, so rsync will create one with the same name as the source.
    // targetDirString = removeLastPathElement(targetDirString);

    //    // Special case: if sending the entire source directory, generate a warning if the source 
    //    // directory name differs from that of the target (project) directory name, since
    //    // it will create a different target directory.
    //    if (sourceDirString.length() == sourceBaseDirStr.length()) {
    //      String sourceDirName = lastPathElement(sourceDirString);
    //      String targetDirName = lastPathElement(targetBaseDir().toString());
    //      if (!sourceDirName.equals(targetDirName)) {
    //        pr("*** Target directory will not lie within the project directory:", INDENT, sourceDirName, CR,
    //            targetBaseDir());
    //      }
    //    }

    {
      StringBuilder sb = new StringBuilder();

      checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
      checkArgument(nonEmpty(ent.url()), "no url:", INDENT, ent);
      sb.append(ent.user());
      sb.append('@');
      sb.append(ent.url());
      sb.append(':');

      sb.append(target);
      s.arg(sb);
    }

    s.assertSuccess();
  }

  /**
   * Determine the source and target paths involved, and detect appropriate
   * problems
   */
  private void determinePaths() {

    if (!isPull()) {

      RemoteEntityInfo remoteEntity = EntityManager.sharedInstance().activeEntity();

      
      
      
      {
        // If no source directory was given, assume the current directory
        //
        File sourceDir = mSourceEntityPath;
        if (Files.empty(sourceDir))
          sourceDir = Files.currentDirectory();

        // The source project directory is the root of the git repository containing the source directory
        //
        mSourceBaseDir = Files.parent(
            Files.getFileWithinParents(sourceDir, ".git", "source git repository containing", sourceDir));

        // The target project directory is found in the remote entity data structure
        //
        mTargetBaseDir = Files.assertAbsolute(remoteEntity.projectDir());

        mSourceRelativeToProject = Files.relativeToContainingDirectory(sourceDir, mSourceBaseDir);
      }
      
      {
        // If no target directory was given, assume 
        File targetDir = mTargetEntityPath;
        if (Files.empty(targetDir)) {
          mTargetRelativeToProject = mSourceRelativeToProject;
        } else {
          if (targetDir.isAbsolute())
            mTargetRelativeToProject = Files.relativeToContainingDirectory(targetDir,
                remoteEntity.projectDir());
          else
            mTargetRelativeToProject = targetDir;
        }
      }

    } else {
      throw notSupported("not finished yet");
    }

    if (verbose()) {
      pr("source arg:", mSourceEntityPath);
      pr("target arg:", mTargetEntityPath);
      pr("source base:", mSourceBaseDir);
      pr("target base:", mTargetBaseDir);
      pr("source rel:", mSourceRelativeToProject);
      pr("target rel:", mTargetRelativeToProject);
    }
  }

  private List<String> excludeExpressionList() {
    if (mExcludeExpressionsList == null) {
      List<String> strings = arrayList();
      mExcludeExpressionsList = strings;
      String script = Files.readString(getClass(), "rsync_excludes.txt");
      for (String s : split(script, '\n')) {
        s = s.trim();
        if (s.isEmpty() || s.startsWith("#"))
          continue;
        strings.add(s);
      }
    }
    return mExcludeExpressionsList;
  }

  //  /**
  //   * Construct EntityPath by parsing a string "[<remote_id>:]<path>]"
  //   */
  //  private EntityPath parseEntityPath(String expr) {
  //    int colon = expr.indexOf(':');
  //    String entityId = null;
  //    String path = expr;
  //    if (colon >= 0) {
  //      entityId = expr.substring(0, colon);
  //      path = expr.substring(colon + 1);
  //    }
  //    checkArgument(path.length() > 0, "can't parse entity path:", expr);
  //
  //    File relPath = new File(path);
  //    if (!relPath.isAbsolute())
  //      relPath = new File(Files.currentDirectory(), relPath.toString());
  //    return EntityPath.newBuilder().entityId(entityId).path(Files.getCanonicalFile(relPath)).build();
  //  }

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

  /**
   * Construct EntityPath by parsing a string "[<remote_id>:]<path>]"
   */
  protected File parseEntityPath(String expr) {
    todo("normalize paths later, when we know which is remote and which is local");
    return new File(expr);
    //    int colon = expr.indexOf(':');
    //    String entityId = null;
    //    String path = expr;
    //    if (colon >= 0) {
    //      entityId = expr.substring(0, colon);
    //      path = expr.substring(colon + 1);
    //    }
    //    checkArgument(path.length() > 0, "can't parse entity path:", expr);
    //
    //    File relPath = new File(path);
    //    if (!relPath.isAbsolute())
    //      relPath = new File(Files.currentDirectory(), relPath.toString());
    //    return EntityPath.newBuilder().entityId(entityId).path(Files.getCanonicalFile(relPath)).build();
  }

  //  // ------------------------------------------------------------------
  //  // Some utility methods for working with paths
  //  // ------------------------------------------------------------------
  //
  //  /**
  //   * Determine index of last '/' within a path. Expects a path that doesn't end
  //   * with '/', and that contains at least two elements.
  //   */
  //
  //  private static int lastPathElementDelimeter(String path) {
  //    int i = path.lastIndexOf('/');
  //    checkState(i > 0 && i < path.length() - 1, "path must have more than one element, and not end with '/':",
  //        path);
  //    return i;
  //  }
  //
  //  /**
  //   * Get the last path element (without '/')
  //   */
  //  private static String lastPathElement(String path) {
  //    return path.substring(lastPathElementDelimeter(path) + 1);
  //  }
  //
  //  /**
  //   * Remove the last path element (and its '/' delimeter)
  //   */
  //  private static String removeLastPathElement(String path) {
  //    return path.substring(0, lastPathElementDelimeter(path));
  //  }
  //
  //  // ------------------------------------------------------------------

  private List<String> mExcludeExpressionsList;
  protected File mSourceEntityPath = Files.DEFAULT;
  protected File mTargetEntityPath = Files.DEFAULT;
  private File mSourceBaseDir;
  private File mTargetBaseDir;
  private File mSourceRelativeToProject;
  private File mTargetRelativeToProject;
}
