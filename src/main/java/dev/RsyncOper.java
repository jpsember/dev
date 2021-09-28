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
import js.json.JSMap;

public class RsyncOper extends AppOper {

  private static final boolean ACT_VERBOSE = true && alert("always verbose for some things");

  @Override
  public String userCommand() {
    return "rsync";
  }

  @Override
  public String getHelpDescription() {
    return "call rsync to synchronize files from source machine to target";
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
      pr("source dir:", mSourceDir);
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    JSMap m = map();
    m.putNumbered("srcDir", sourceDir().toString());
    m.putNumbered("srcBaseDir", sourceBaseDir().toString());
    m.putNumbered("relPath", relPath().toString());
    m.putNumbered("trgBaseDir", targetBaseDir().toString());
    pr(m);

    SystemCall s = new SystemCall();
    boolean verbosity = verbose() || ACT_VERBOSE;
    s.withVerbose(verbosity);
    s.arg("rsync", "--archive");
    if (verbosity)
      s.arg("--verbose");
    if (dryRun() || alert("always dry run"))
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

    s.arg(sourceDir());

    RemoteEntityInfo ent = EntityManager.sharedInstance().activeEntity();
    checkArgument(ent.port() > 0, "bad port:", INDENT, ent);

    s.arg("-e", "ssh -p" + ent.port());

    String rp = relPath();
    // Trim the last path element (everything from the last '/' onward)
    {
      int i = rp.lastIndexOf('/');
      if (i < 0)
        rp = "";
      else
        rp = rp.substring(0, i);
    }

    checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
    checkArgument(nonEmpty(ent.url()), "no url:", INDENT, ent);
    String remotePrefix = ent.user() + "@" + ent.url() + ":";

    s.arg(remotePrefix + targetBaseDir().toString() + "/" + rp);
    s.call();

    {
      m = s.toJson();
      pr("Output:", INDENT, m.get("system_out"));
      pr("Command:", INDENT, m.get("args"));
      //pr(m);
    }
    s.assertSuccess();
  }

  private String relPath() {
    if (mRelPath == null) {
      String base = sourceBaseDir().toString();
      String subdir = sourceDir().toString();
      checkArgument(subdir.startsWith(base), "expected source directory", subdir,
          "to be a subdirectory of base", base);

      checkArgument(subdir.length() > base.length(), "subdirectory is not a proper subdirectory of:", base);
      String sourceRelPath = subdir.substring(base.length());

      mRelPath = sourceRelPath.substring(1);
    }
    return mRelPath;
  }

  private File sourceDir() {
    if (mSourceDir == null) {
      mSourceDir = currentDir();
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

  private File currentDir() {
    if (true) {
      pr("...current dir:", Files.currentDirectory());
      return Files.currentDirectory();
    }
    todo("this is temporary, since Eclipse work directory ISN'T available from Files.currentDirectory()");
    return new File(Files.homeDirectory(), "eio_consulting/barnserv_alpha1/jv_exp/src/main");
  }

  private File targetBaseDir() {
    if (mTargetBaseDir == null) {
      // Assume the target base directory is the home directory + repository name (the last element of the repo base dir)
      mTargetBaseDir = new File(sourceBaseDir().getName());
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

  private List<String> mExcludeExpressionsList;
  private File mSourceDir;
  private File mSourceBaseDir;
  private String mRelPath;
  private File mTargetBaseDir;
}
