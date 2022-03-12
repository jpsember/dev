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
import java.util.regex.Pattern;

import dev.gen.AppInfo;
import js.app.AppOper;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;
import js.parsing.MacroParser.Mapper;
import js.parsing.RegExp;

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
    parsePomFile();
    createBuildScript();
    createDriver();
  }

  private AppInfo.Builder appInfo() {
    if (mAppInfo == null) {
      mAppInfo = AppInfo.newBuilder();
      {
        File pomFile = Files.getFileWithinParents(null, "pom.xml", "determining project directory");
        mAppInfo.pomFile(pomFile);
        mAppInfo.dir(Files.parent(pomFile));
      }
      log("...derived app info:", INDENT, mAppInfo);
    }
    return mAppInfo;
  }

  private void postProcessArgs() {
    appInfo();
  }

  private void parsePomFile() {
    // Look for a JSMap embedded within an xml comment with prefix "<!--DEV" 
    String prefix = "<!--DEV";
    String suffix = "-->";

    String content = Files.readString(appInfo().pomFile());

    int commentStart = content.indexOf(prefix);
    if (commentStart < 0) {
      log("cannot locate arguments from pom.xml with prefix:", prefix, "; assuming no driver required");
      return;
    }
    checkState(mDriver == null);
    mDriver = true;

    int commentEnd = content.indexOf(suffix, commentStart);
    if (commentEnd < 0)
      badArg("Can't find end of comment tag in pom.xml");

    String jsonContent = content.substring(commentStart + prefix.length(), commentEnd);
    JSMap m = new JSMap(jsonContent);
    log("Parameters parsed from pom.xml:", INDENT, m);

    String key = "cmdline";
    String cmdline = m.opt(key, "");
    if (!cmdline.isEmpty()) {
      m.put(key, processCommandLineParameter(cmdline));
    }
    mPomParametersMap = m;

    log("result:", m);

    // Replace the content with our modified version of the map

    String newContent = content.substring(0, commentStart + prefix.length()) + "\n\n" + m.prettyPrint() + "\n"
        + content.substring(commentEnd);

    setTarget("pom.xml");
    writeTargetIfChanged(newContent, false);
  }

  private String processCommandLineParameter(String content) {
    List<String> args = split(content, ' ');
    List<String> filtered = arrayList();

    int argNum = 0;
    for (String expr : args) {
      switch (argNum) {
      case 0:
        checkArgument(expr.endsWith("java"), "expected argument to invoke java:", expr);
        filtered.add("java");
        argNum++;
        break;

      default:
        String prev = last(filtered);
        if (prev.equals("-classpath")) {
          expr = processClassPathArg(expr);
        }
        filtered.add(expr);
        break;
      }

    }

    String extraArgsArgument = quote("$@");
    if (!filtered.contains(extraArgsArgument))
      filtered.add(extraArgsArgument);

    return String.join(" ", filtered);
  }

  private String processClassPathArg(String content) {
    List<String> args = split(content, ':');
    List<String> filtered = arrayList();
    for (String expr : args) {

      // Determine if this classpath entry refers to a project within the maven repository

      do {
        // Does it explicitly refer to a maven repository?
        //
        String seek = ".m2/";
        int c = expr.indexOf(seek);
        if (c >= 0) {
          expr = "$MVN/" + expr.substring(c + seek.length());
          break;
        }

        // Does it seem to refer to a project for which we have maven project?
        // Maybe we can assume *all* projects are within the maven repository...
        seek = "/target/classes";
        c = expr.indexOf(seek);
        if (c >= 0) {

          // Assume this refers to a project with a pom file
          String dir = expr.substring(0, c);
          log("...inferring maven location from:", dir);
          File pomFile = new File(dir + "/pom.xml");
          if (!pomFile.exists()) {
            badArg("can't locate pom.xml:", pomFile, INDENT, "for expression:", expr);
          }
          String pathRelToMaven = extractClassesPathFromPom(pomFile);
          expr = "$MVN/" + pathRelToMaven;
          break;
        }

        pr("*** Failed to figure out expression:", expr);
      } while (false);
      filtered.add(expr);
    }
    return String.join(":", filtered);
  }

  private static String parsePomTag(String content, String tag) {
    try {
      String t2 = "<" + tag + ">";
      int i = content.indexOf(t2);
      int j = content.indexOf("</" + tag + ">", i);
      String x = content.substring(i + t2.length(), j);
      checkArgument(RegExp.patternMatchesString("[\\w.]+", x));
      return x;
    } catch (Throwable t) {
      throw badArg("Failed to parse tag from pom file:", tag);
    }
  }

  private String extractClassesPathFromPom(File pomFile) {
    String content = Files.readString(pomFile);
    String groupId = parsePomTag(content, "groupId");
    String artifactId = parsePomTag(content, "artifactId");
    String version = parsePomTag(content, "version");
    String exp = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-"
        + version + ".jar";
    return exp;
  }

  private String appName() {
    return appInfo().name();
  }

  private File appDir() {
    return Files.assertNonEmpty(appInfo().dir(), "appInfo.dir");
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

  private void writeTargetIfChanged(String content, boolean executable) {
    File targ = mTargetFile;
    if (alert("*** adding extra prefix"))
      targ = new File(targ.getParentFile(), "_temp_" + targ.getName());
    files().mkdirs(Files.parent(targ));
    if (verbose())
      log("writing to:", targ, INDENT, debStr(content));
    files().writeIfChanged(targ, content);
    if (executable)
      files().chmod(targ, 744);
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

  private boolean driverRequired() {
    if (mDriver == null)
      throw badState("no driver flag determined yet");
    return mDriver;
  }

  private void createBuildScript() {
    setTarget("make_new.sh");
    String template = frag("mk_template.txt");
    template = modifyTemplateWithExistingCustomizations(template);
    if (!driverRequired())
      template = template.replace("DRIVER=1", "DRIVER=0");
    else
      template = template.replace("DRIVER=0", "DRIVER=1");
    String result = parseText(template);
    writeTargetIfChanged(result, true);
  }

  private void createDriver() {
    if (!driverRequired())
      return;
    setTarget("driver.sh");
    String template = frag("driver2_template.txt");
    macroMap().put("run_app_command", mPomParametersMap.get("cmdline"));
    template = modifyTemplateWithExistingCustomizations(template);
    String result = parseText(template);
    writeTargetIfChanged(result, true);
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

  private Boolean mDriver;
  private JSMap mMacroMap;
  private AppInfo.Builder mAppInfo;
  private JSMap mPomParametersMap;

}
