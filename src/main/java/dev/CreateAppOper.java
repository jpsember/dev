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

import dev.gen.CreateAppConfig;
import js.app.AppOper;
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
  public String getHelpDescription() {
    return "create Java app";
  }

  @Override
  public void perform() {
    postProcessArgs();
    createPom();
    createSource();
    createDatFiles();
    compileDatFiles();
    createGitIgnore();
    createInstallJson();
  }

  @Override
  protected void getOperSpecificHelp(BasePrinter b) {
    b.pr("Create a directory to hold the project, and from that directory, type 'dev createapp package ...");
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
    {
      var f = Files.currentDirectory().getName();
      if (!RegExp.patternMatchesString("[a-z]+", f))
        badArg("name of current directory is not appropriate for an app name:", f);
      c.name(f);
    }

    if (c.mainClassName().isEmpty())
      c.mainClassName(c.name());

    {
      var s = c.mainClassName();
      if (s.endsWith(".java"))
        badArg("Extraneous '.java' suffix for", s);
    }

    if (c.mainPackage().isEmpty())
      c.mainPackage(c.name());

    {
      // If the current directory contains other directories, or suspicious files, report an error
      var d = new DirWalk(Files.currentDirectory()).includeDirectories().withRecurse(true);
      for (var f : d.files()) {
        if (f.isDirectory()) {
          badArg("Unexpected current directory contents:", INDENT, Files.infoMap(f), CR,
              "You must run this command from the directory that is to contain the new app");
        }
      }
    }
    {
      String mainPackage = c.mainPackage();
      String suffix = "." + c.mainClassName();
      if (mainPackage.endsWith(suffix))
        badArg("Extraneous suffix for <package> arg:", suffix);
      if (!("." + mainPackage).endsWith("." + c.name()))
        badArg("*** Package", quote(mainPackage), "doesn't end with app name", quote(c.name()));
      if (nonEmpty(c.mainPackage()))
        checkArgumentsEqual(mainPackage, c.mainPackage(), "Inferred package vs command line arg");
    }
  }

  private File mainJavaFile() {
    if (mMainJavaFile == null) {
      mMainJavaFile = appFile("src/main/java/" + AppUtil.dotToSlash(config().mainPackage()) + "/"
          + config().mainClassName() + ".java");
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
    return Files.readString(getClass(), "createapp/" + resourceName);
  }

  private File appFile(String pathRelativeToProject) {
    return new File(pathRelativeToProject);
  }

  private void createPom() {
    setTarget("pom.xml");
    writeTargetIfMissing(parseResource("pom_template.xml"));
  }

  private void writeTargetIfMissing(String content) {
    if (!targetFile().exists())
      writeFile(targetFile(), content);
  }

  private File writeFile(File path, String content) {
    // If path has a parent directory, create it if necessary
    File dir = path.getParentFile();
    if (dir != null)
      files().mkdirs(dir);
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
      m.put("package_name", c.mainPackage());
      m.put("package_name_slashes", c.mainPackage().replace('.', '/'));
      m.put("main_class_name", c.mainClassName());
      m.put("main_class", c.mainPackage() + "." + c.mainClassName());
      m.put("main_oper_name", c.mainClassName() + "Oper");
      m.put("test_package_name", c.mainPackage());
      m.put("test_class_name", testClassName());
      m.put("pom_dependencies", frag("pom_dependencies.xml"));
      m.put("datagen_gitignore_comment", "# ...add appropriate entries for generated Java files");

      if (c.omitJsonArgs()) {
        m.put("config_import_statement", "");
        m.put("json_args_support", "");
        m.put("config_class", "");
      } else {
        var configDatName = c.name() + "_config";
        var configDatNameJava = DataUtil.convertUnderscoresToCamelCase(configDatName);
        m.put("config_import_statement", "import dfa.gen." + configDatNameJava + ";");
        m.put("json_args_support", frag("json_args_java.txt"));
        m.put("config_class", configDatNameJava);
      }
      m.lock();
      mMacroMap = m;
    }
    return mMacroMap;
  }

  private void createSource() {
    mTargetFile = mainJavaFile();
    writeTargetIfMissing(parseResource("main_java.txt"));
    mTargetFile = new File(chomp(mainJavaFile().toString(), ".java") + "Oper.java");
    writeTargetIfMissing(parseResource("main_oper.txt"));

    String testSubdir = "src/test/java";
    File testDir = appFile(testSubdir);
    // If there are already Java files in the test directory, don't write any 'do nothing' tests
    if (!testDir.exists() || new DirWalk(testDir).withExtensions("java").files().isEmpty()) {
      setTarget(
          testSubdir + "/" + AppUtil.dotToSlash(config().mainPackage()) + "/" + testClassName() + ".java");
      writeTargetIfMissing(parseResource("main_test_java.txt"));
    }
  }

  private void createDatFiles() {
    var genDir = "dat_files/" + AppUtil.dotToSlash(config().mainPackage()) + "/gen/";
    setTarget(genDir + "_SKIP_sample.dat.RENAME_ME");
    writeTargetIfMissing(parseResource("sample_dat.txt"));
    if (!config().omitJsonArgs()) {
      var datName = config().name() + "_config";
      setTarget(genDir + datName + ".dat");
      writeTargetIfMissing(parseResource("config_dat.txt"));
    }
  }

  private void compileDatFiles() {
    var s = new SystemCall();
    s.setVerbose(verbose());
    s.arg("datagen");
    log("attempting generate data classes");
    s.assertSuccess();
  }

  private void createGitIgnore() {
    setTarget(".gitignore");
    writeTargetIfMissing(parseResource("gitignore.txt"));
  }

  private void createInstallJson() {
    setTarget("install.json");
    writeTargetIfMissing(parseResource("install_template.json"));
  }

  // ------------------------------------------------------------------
  // A distinguished 'target file'

  private void setTarget(String pathRelativeToProject) {
    mTargetFile = appFile(pathRelativeToProject);
  }

  private File targetFile() {
    return mTargetFile;
  }

  private File mTargetFile;
  //------------------------------------------------------------------

  private File mMainJavaFile;
  private JSMap mMacroMap;
  private CreateAppConfig.Builder mConfig;

}
