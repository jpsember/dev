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

import dev.gen.AppInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;

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
    createAppDir();
    createPom();
    createSource();
    createDatFiles();
    createGitIgnore();
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[ package <xxx.yyy.etc> | startdir <dir> | main <e.g. Main> ]*");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    mStartDirString = args.nextArgIf("startdir", mStartDirString);
    mMainPackageArg = args.nextArgIf("package", mMainPackageArg);
    mMainClassName = args.nextArgIf("main", mMainClassName);
  }

  private AppInfo.Builder appInfo() {
    if (mAppInfo == null) {
      mAppInfo = AppInfo.newBuilder();

      File startDir;
      if (mStartDirString.isEmpty())
        startDir = Files.currentDirectory();
      else
        startDir = new File(mStartDirString);

      AppUtil.withDirectory(mAppInfo, startDir, mMainClassName);
      log("...derived app info:", INDENT, mAppInfo);
    }
    return mAppInfo;
  }

  private void postProcessArgs() {
    {
      String mainClassName = mMainClassName;
      if (mainClassName.endsWith(".java"))
        badArg("Extraneous '.java' suffix for", mainClassName);
    }
    {
      if (appInfo().name().equals("source"))
        badArg("App name is", quote(appInfo().name()),
            "; you need to start in an appropriate subdirectory of the source directory");
    }
    {
      String mainPackage = mMainPackageArg;
      String suffix = "." + mMainClassName;
      if (mainPackage.endsWith(suffix))
        badArg("Extraneous suffix for <package> arg:", suffix);
      if (!("." + mainPackage).endsWith("." + appInfo().name()))
        badArg("*** Package", quote(mainPackage), "doesn't end with app name", quote(appInfo().name()));

      if (nonEmpty(appInfo().mainPackage()))
        checkArgumentsEqual(mainPackage, appInfo().mainPackage(), "Inferred package vs command line arg");
      appInfo().mainPackage(mainPackage);
    }
  }

  private String appName() {
    return appInfo().name();
  }

  private File appDir() {
    return appInfo().dir();
  }

  private File mainJavaFile() {
    if (mMainJavaFile == null) {
      mMainJavaFile = appFile(
          "src/main/java/" + AppUtil.dotToSlash(appInfo().mainPackage()) + "/" + mMainClassName + ".java");
    }
    return mMainJavaFile;
  }

  private void createAppDir() {
    files().mkdirs(appDir());
  }

  private String parseResource(String resourceName) {
    return parseText(frag(resourceName));
  }

  private String parseText(String template) {
    MacroParser parser = new MacroParser();
    parser.withTemplate(template).withMapper(macroMap());
    return parser.content();
  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), resourceName);
  }

  private File appFile(String pathRelativeToProject) {
    return new File(appDir(), pathRelativeToProject);
  }

  private void createPom() {
    setTarget("pom.xml");
    writeTargetIfMissing(parseResource("pom_template.xml") );
  }

  private void writeTargetIfMissing(String content) {
    if (!targetFile().exists())
      writeFile(targetFile(), content);
  }

  private File writeFile(File path, String content) {
    files().mkdirs(Files.parent(path));
    files().writeString(path, content);
    return path;
  }

  private String testClassName() {
    return appName().substring(0, 1).toUpperCase() + appName().substring(1) + "Test";
  }

  private JSMap macroMap() {
    if (mMacroMap == null) {
      JSMap m = map();
      m.put("group_id", "com.jsbase");
      m.put("app_name", appName());
      m.put("package_name", appInfo().mainPackage());
      m.put("package_name_slashes", appInfo().mainPackage().replace('.', '/'));
      m.put("main_class_name", appInfo().mainClassName());
      m.put("main_oper_name", appInfo().mainClassName() + "Oper");
      m.put("test_package_name", appInfo().mainPackage());
      m.put("test_class_name", testClassName());
      m.put("pom_dependencies", frag("pom_dependencies.xml"));
      m.put("datagen_gitignore_comment", "# ...add appropriate entries for generated Java files");
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
          testSubdir + "/" + AppUtil.dotToSlash(appInfo().mainPackage()) + "/" + testClassName() + ".java");
      writeTargetIfMissing(parseResource("main_test_java.txt"));
    }
  }

  private void createDatFiles() {
    setTarget("dat_files/" + AppUtil.dotToSlash(appInfo().mainPackage()) + "/gen/_SKIP_sample.dat.RENAME_ME");
    writeTargetIfMissing(parseResource("sample_dat.txt"));
  }

  private void createGitIgnore() {
    setTarget(".gitignore");
    writeTargetIfMissing(parseResource("gitignore.txt"));
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

  private String mStartDirString = "";
  private String mMainPackageArg = "";
  private String mMainClassName = "Main";
  private File mMainJavaFile;
  private JSMap mMacroMap;
  private AppInfo.Builder mAppInfo;

}
