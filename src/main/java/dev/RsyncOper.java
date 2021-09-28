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

public class RsyncOper extends AppOper {

  @Override
  public String userCommand() {
    return "rsync";
  }

  @Override
  public String getHelpDescription() {
    return "call rsync to synchronize files from source machine to target";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[<source dir>]", "[<target dir>]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (args.hasNextArg()) {
      File relPath = new File(args.nextArg());
      if (!relPath.isAbsolute()) {
        relPath = new File(Files.currentDirectory(), relPath.toString());
      }
      mSourceDir = Files.getCanonicalFile(relPath);
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
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

    // Construct an exclude list within a temporary file, and pass that as an argument
    //
    {
      File tempFile;
      if (verbosity)
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
      s.arg("--exclude-from=" + tempFile);
    }

    String sourceBaseDirStr = sourceBaseDir().toString();
    String sourceDirString = sourceDir().toString();
    s.arg(sourceDirString);

    RemoteEntityInfo ent = EntityManager.sharedInstance().activeEntity();
    checkArgument(ent.port() > 0, "bad port:", INDENT, ent);

    s.arg("-e", "ssh -p" + ent.port());

    // Determine the target directory

    String targetDirString;
    {
      checkArgument(sourceDirString.startsWith(sourceBaseDirStr));
      String sourceOffsetString = sourceDirString.substring(sourceBaseDirStr.length());
      targetDirString = targetBaseDir() + sourceOffsetString;
    }

    // Omit the target directory name, so rsync will create one with the same name as the source.
    targetDirString = removeLastPathElement(targetDirString);

    // Special case: if sending the entire source directory, generate a warning if the source 
    // directory name differs from that of the target (project) directory name, since
    // it will create a different target directory.
    if (sourceDirString.length() == sourceBaseDirStr.length()) {
      String sourceDirName = lastPathElement(sourceDirString);
      String targetDirName = lastPathElement(targetBaseDir().toString());
      if (!sourceDirName.equals(targetDirName)) {
        pr("*** Target directory will not lie within the project directory:", INDENT, sourceDirName, CR,
            targetBaseDir());
      }
    }

    {
      StringBuilder sb = new StringBuilder();

      checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
      checkArgument(nonEmpty(ent.url()), "no url:", INDENT, ent);
      sb.append(ent.user());
      sb.append('@');
      sb.append(ent.url());
      sb.append(':');

      sb.append(targetDirString);
      s.arg(sb);
    }

    s.assertSuccess();
  }

  private File sourceDir() {
    if (mSourceDir == null) {
      mSourceDir = Files.currentDirectory();
    }
    return mSourceDir;
  }

  private File sourceBaseDir() {
    if (mSourceBaseDir == null) {
      File srcDir = sourceDir();
      mSourceBaseDir = Files
          .parent(Files.getFileWithinParents(srcDir, ".git", "source git repository containing", srcDir));
    }
    return mSourceBaseDir;
  }

  private File targetBaseDir() {
    if (mTargetBaseDir == null) {
      RemoteEntityInfo ent = EntityManager.sharedInstance().activeEntity();
      mTargetBaseDir = Files.assertAbsolute(ent.projectDir());
    }
    return mTargetBaseDir;
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

  // ------------------------------------------------------------------
  // Some utility methods for working with paths
  // ------------------------------------------------------------------

  /**
   * Determine index of last '/' within a path. Expects a path that doesn't end
   * with '/', and that contains at least two elements.
   */

  private static int lastPathElementDelimeter(String path) {
    int i = path.lastIndexOf('/');
    checkState(i > 0 && i < path.length() - 1, "path must have more than one element, and not end with '/':",
        path);
    return i;
  }

  /**
   * Get the last path element (without '/')
   */
  private static String lastPathElement(String path) {
    return path.substring(lastPathElementDelimeter(path) + 1);
  }

  /**
   * Remove the last path element (and its '/' delimeter)
   */
  private static String removeLastPathElement(String path) {
    return path.substring(0, lastPathElementDelimeter(path));
  }

  // ------------------------------------------------------------------

  private List<String> mExcludeExpressionsList;
  private File mSourceDir;
  private File mSourceBaseDir;
  private File mTargetBaseDir;
}
