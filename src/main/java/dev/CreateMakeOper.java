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
import java.util.regex.Pattern;

import dev.gen.AppInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;
import js.parsing.MacroParser.Mapper;

public final class CreateMakeOper extends AppOper {

  @Override
  public String userCommand() {
    return "createmake";
  }

  @Override
  public String getHelpDescription() {
    return "create Java project make and related files";
  }

  @Override
  public void perform() {
    postProcessArgs();
    createBuildScript();
  }

  private AppInfo.Builder appInfo() {
    if (mAppInfo == null) {
      mAppInfo = AppInfo.newBuilder();

      todo("figure out project directory, pom file, etc");
      log("...derived app info:", INDENT, mAppInfo);
    }
    return mAppInfo;
  }

  private void postProcessArgs() {
    appInfo();
    log("app info:", appInfo());
  }

  private String appName() {
    return appInfo().name();
  }

  private File appDir() {
    return Files.assertNonEmpty(appInfo().dir(), "appInfo.dir");
  }

  private boolean generateDriver() {
    return mDriver;
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

  private void writeTarget(String content) {
    halt("attempt to write content to:", targetFile());
    writeFile(targetFile(), content);
  }

  private File writeFile(File path, String content) {
    files().mkdirs(Files.parent(path));
    files().writeString(path, content);
    if (mExecutable) {
      mExecutable = false;
      if (!files().dryRun()) {
        path.setExecutable(true);
      }
    }
    return path;
  }

  private static String keyPrefix(String macroKey) {
    int k = macroKey.indexOf(':');
    checkArgument(k > 0);
    return macroKey.substring(0, k);
  }

  private final Pattern CUSTOMIZATIONS_MACRO_EXPR = Pattern.compile("(\\{~[a-zA-Z0-9]+:[^~]*~\\})");

  /**
   * Given a template, if target file already exists, incorporate its
   * customizations
   */
  private String modifyTemplateWithExistingCustomizations(String template) {
    String oldContent = Files.readString(targetFile(), "");
    MacroParser parser = new MacroParser().withPattern(CUSTOMIZATIONS_MACRO_EXPR);

    // Read old customizations
    JSMap oldCustomMap = map();
    parser.withTemplate(oldContent).content(new Mapper() {
      @Override
      public String textForKey(String key) {
        String prefix = keyPrefix(key);
        if (oldCustomMap.containsKey(prefix))
          die("duplicate macro key:", prefix);
        oldCustomMap.put(prefix, key);
        return key;
      }
    });

    // Insert customizations into template
    parser = new MacroParser().withPattern(CUSTOMIZATIONS_MACRO_EXPR);
    template = parser.withTemplate(template).content(new Mapper() {
      @Override
      public String textForKey(String key) {
        String prefix = keyPrefix(key);
        return oldCustomMap.opt(prefix, key);
      }
    });
    return template;
  }

  private void createBuildScript() {
    setTarget("mk");
    String template = frag("mk_template.txt");
    template = modifyTemplateWithExistingCustomizations(template);
    if (!generateDriver())
      template = template.replace("DRIVER=1", "DRIVER=0");
    else
      template = template.replace("DRIVER=0", "DRIVER=1");
    String result = parseText(template);
    writeTarget(result);
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
      m.put("driver_name", appInfo().mainClassName());
      m.put("test_package_name", appInfo().mainPackage());

      m.put("link_define", frag("link_define.txt"));
      m.put("link_clean", frag("link_clean.txt"));
      m.put("link_create", frag("link_create.txt"));

      m.put("datagen_clean", frag("datagen_clean.txt"));
      m.put("datagen_build", frag("datagen_build.txt"));
      m.put("datagen_gitignore_comment", "# ...add appropriate entries for generated Java files");

      m.put("pom_dependencies", frag("pom_dependencies.xml"));

      m.lock();
      mMacroMap = m;
    }
    return mMacroMap;
  }

  // ------------------------------------------------------------------
  // A distinguished 'target file'

  private void setTarget(String pathRelativeToProject) {
    mTargetFile = appFile(pathRelativeToProject);
  }

  private File targetFile() {
    return Files.assertNonEmpty(mTargetFile, "targetFile");
  }

  private File mTargetFile;
  //------------------------------------------------------------------

  private boolean mDriver;
  private JSMap mMacroMap;
  private boolean mExecutable;
  private AppInfo.Builder mAppInfo;

}
