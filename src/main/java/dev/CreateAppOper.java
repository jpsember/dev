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
import java.util.Map;

import dev.gen.CreateAppConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;
import js.parsing.RegExp;

public final class CreateAppOper extends AppOper {

  @Override
  public String userCommand() {
    return "createapp";
  }

  @Override
  public String shortHelp() {
    return "create Java app";
  }

  @Override
  public void perform() {
    postProcessArgs();
    createPom();
    createSource();
    createDatFiles();
    compileDatFiles();
    createGitRepo();
    createInstallJson();
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("Create a directory to hold the project, and from that directory, type 'dev createapp <options>");
    b.pr("where <options> include:");
    var hf = new HelpFormatter();
    hf.addItem("[ parent_dir <path> ]", "directory to contain project");
    hf.addItem("[ name <string> ]", "name of project");
    hf.addItem("[ zap_existing <directory> ]", "deleting existing directory before starting");
    b.pr(hf);
  }

  @Override
  public CreateAppConfig defaultArgs() {
    return CreateAppConfig.DEFAULT_INSTANCE;
  }

  @Override
  public CreateAppConfig.Builder config() {
    if (mConfig == null)
      mConfig = ((CreateAppConfig) super.config()).toBuilder();
    return mConfig;
  }

  private void postProcessArgs() {
    var c = config();
    log("post processing args; config:", c);

    {
      var d = c.parentDir();
      if (Files.empty(d))
        d = Files.currentDirectory();
      c.parentDir(Files.absolute(d));
    }

    {
      var name = readIfMissing(c.name());
      if (name.isEmpty()) {
        name = Files.basename(c.parentDir());
      }
      if (name.isEmpty())
        badArg("missing arg: name");
      if (!RegExp.patternMatchesString("[a-z]+", name))
        badArg("inappropriate app name:", name);
      mAppDir = new File(c.parentDir(), name);
      c.name(name);
    }
    {
      var z = c.zapExisting();
      if (Files.nonEmpty(z)) {
        if (!z.equals(mAppDir))
          badArg("zap_existing is nonempty, but not equal to app dir:", mAppDir, INDENT, c);
        files().deleteDirectory(z, "/" + c.name());
      }
    }
    files().mkdirs(mAppDir);

    mMainPackage = c.name();
    mMainClassName = DataUtil.capitalizeFirst(c.name());
  }

  private File mainJavaFile() {
    if (mMainJavaFile == null) {
      mMainJavaFile = appFile("src/main/java/" + mMainPackage + "/" + mMainClassName + ".java");
    }
    return mMainJavaFile;
  }

  private String parseResource(String resourceName) {
    return parseText(frag(resourceName));
  }

  private String parseText(String template) {
    // Keep applying the parser until the content doesn't change
    var orig = template;
    while (true) {
      MacroParser parser = new MacroParser();
      parser.withTemplate(orig).withMapper(macroMap());
      var result = parser.content();
      if (result.equals(orig))
        return orig;
      orig = result;
    }
  }

  private String frag(String resourceName) {
    var path = "createapp/" + resourceName;
    return Files.readString(getClass(), path);
  }

  private File appFile(String pathRelativeToProject) {
    return new File(mAppDir, pathRelativeToProject);
  }

  private void createPom() {
    setTarget("pom.xml");
    write(parseResource("pom_template.xml"));
  }

  private void write(String content) {
    var f = targetFile();
    checkState(!f.exists());
    writeFile(f, content);
  }

  private File writeFile(File path, String content) {
    files().mkdirs(Files.parent(path));
    files().writeString(path, content);
    return path;
  }

  private String testClassName() {
    return DataUtil.capitalizeFirst(config().name()) + "Test";
  }

  private JSMap macroMap() {
    if (mMacroMap == null) {
      var c = config();
      JSMap m = map();
      m.put("group_id", "com.jsbase");
      m.put("app_name", c.name());
      m.put("package_name", mMainPackage);
      m.put("package_name_slashes", mMainPackage.replace('.', '/'));
      m.put("main_class_name", mMainClassName);
      m.put("main_class", mMainPackage + "." + mMainClassName);
      m.put("main_oper_name", mMainClassName + "Oper");
      m.put("test_package_name", mMainPackage);
      m.put("test_class_name", testClassName());
      {
        var s = frag("pom_dependencies.xml");
        m.put("pom_dependencies", s);
      }
      m.put("datagen_gitignore_comment", "# ...add appropriate entries for generated Java files");

      for (var expr : split("java-core java-testutil", ' ')) {
        var url = "github.com/jpsember/" + expr;
        var tag = mostRecentTagForRepo(url);
        log("most recent tag for", url, "is", tag);
        m.put(expr, tag);
      }

      if (c.omitJsonArgs()) {
        m.put("config_import_statement", "");
        m.put("json_args_support", "");
        m.put("config_class", "");
      } else {
        var configDatName = c.name() + "_config";
        var configDatNameJava = DataUtil.convertUnderscoresToCamelCase(configDatName);
        m.put("config_import_statement", "import " + mMainPackage + ".gen." + configDatNameJava + ";");
        m.put("json_args_support", frag("json_args_java.txt"));
        m.put("config_class", configDatNameJava);
      }
      m.lock();
      mMacroMap = m;
    }
    return mMacroMap;
  }

  private void createSource() {
    setTarget(mainJavaFile());
    write(parseResource("main_java.txt"));
    mTargetFile = new File(chomp(mainJavaFile().toString(), ".java") + "Oper.java");
    write(parseResource("main_oper.txt"));

    String testSubdir = "src/test/java";
    File testDir = appFile(testSubdir);
    // If there are already Java files in the test directory, don't write any 'do nothing' tests
    if (!testDir.exists() || new DirWalk(testDir).withExtensions("java").files().isEmpty()) {
      setTarget(testSubdir + "/" + AppUtil.dotToSlash(mMainPackage) + "/" + testClassName() + ".java");
      write(parseResource("main_test_java.txt"));
    }
  }

  private void createDatFiles() {
    var genDir = "dat_files/" + AppUtil.dotToSlash(mMainPackage) + "/gen/";
    if (config().omitJsonArgs()) {
      setTarget(genDir + "_SKIP_sample.dat.RENAME_ME");
      write(parseResource("sample_dat.txt"));
    } else {
      var datName = config().name() + "_config";
      setTarget(genDir + datName + ".dat");
      write(parseResource("config_dat.txt"));
    }
  }

  private void compileDatFiles() {
    var s = sysCall();
    s.arg("/usr/local/bin/datagen");
    s.arg("start_dir", mAppDir);
    log("attempting generate data classes");
    s.assertSuccess();
  }

  private SystemCall sysCall() {
    var s = new SystemCall();
    s.setVerbose(verbose());
    // Execute all commands with mAppDir as the current directory
    s.directory(mAppDir);
    return s;
  }

  private void createGitRepo() {
    setTarget(".gitignore");
    write(parseResource("gitignore.txt"));
    sysCall().arg("git", "init", "-b", "main").assertSuccess();
    sysCall().arg("git", "add", ".gitignore").assertSuccess();
    sysCall().arg("git", "commit", "-m", "initial commit").assertSuccess();
  }

  private void createInstallJson() {
    setTarget("install.json");
    write(parseResource("install_template.json"));
  }

  private String mostRecentTagForRepo(String repoUrl) {
    var tag = mRepoTagMap.get(repoUrl);
    if (tag == null) {
      if (testMode()) {
        tag = "9.9";
      } else {
        var s = new SystemCall();
        s.setVerbose(verbose());
        s.arg("git", "ls-remote", "--tags", "-q", "--sort=-v:refname", "--refs", "https://" + repoUrl);
        s.assertSuccess();
        var out = s.systemOut();
        var x = split(out, '\n');
        tag = "";
        if (!x.isEmpty()) {
          var w = x.get(0);
          var j = w.lastIndexOf('/');
          checkArgument(j > 0, "failed to parse tags from:", INDENT, out);
          tag = w.substring(j + 1);
        }
      }
      mRepoTagMap.put(repoUrl, tag);
    }
    return tag;
  }

  private Map<String, String> mRepoTagMap = hashMap();

  // ------------------------------------------------------------------
  // A distinguished 'target file'

  private void setTarget(File file) {
    mTargetFile = file;
  }

  private void setTarget(String pathRelativeToProject) {
    setTarget(appFile(pathRelativeToProject));
  }

  private File targetFile() {
    checkState(Files.nonEmpty(mTargetFile), "no targetFile defined");
    return mTargetFile;
  }

  private File mTargetFile;
  //------------------------------------------------------------------

  private File mMainJavaFile;
  private JSMap mMacroMap;
  private CreateAppConfig.Builder mConfig;
  private File mAppDir;
  private String mMainPackage;
  private String mMainClassName;
}
