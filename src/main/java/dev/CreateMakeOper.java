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

import dev.gen.AppInfo;
import dev.gen.DependencyEntry;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;

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
    createBinMk();
    if (mBinOnlyFlag)
      return;
    parsePomFile();
    createBuildScript();
    if (mDriver)
      createDriver();
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[bin]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    mBinOnlyFlag = args.nextArgIf("bin");
  }

  private AppInfo.Builder appInfo() {
    if (mAppInfo == null) {
      mAppInfo = AppInfo.newBuilder();
      File pomFile = Files.getFileWithinParents(null, "pom.xml", "determining project directory");
      mAppInfo.pomFile(pomFile);
      mAppInfo.dir(Files.parent(pomFile));
      log("...derived app info:", INDENT, mAppInfo);
    }
    return mAppInfo;
  }

  private String parsePomProperty(String propertyName) {
    String result = null;
    String pomContent = mPomContent;
    String expr = "<" + propertyName + ">";
    int c = optIndexOf(pomContent, expr, 0);
    if (c >= 0) {
      int nameStart = c + expr.length();
      int c2 = indexOf(pomContent, "<", nameStart);
      result = pomContent.substring(nameStart, c2);
    }
    log("parsePomProperty", expr, "=>", result);
    return result;
  }

  private void parsePomFile() {
    mPomContent = Files.readString(appInfo().pomFile());
    String driverName = parsePomProperty("driver.name");
    determineMainClass();

    // If no explicit driver name was given, and a main class was found,
    // derive a suitable driver name from the main class's package
    //
    if (nonEmpty(mMainClass) && nullOrEmpty(driverName)) {
      List<String> packageElements = split(mMainClass, '.');
      if (packageElements.size() >= 2) {
        String element = getMod(packageElements, -2);
        if (element.toLowerCase().equals(element)) {
          driverName = element;
        }
      }
    }
    if (nonEmpty(mMainClass) && nullOrEmpty(driverName))
      throw badArg("no driver name available");
    mDriver = nonEmpty(driverName);
    if (mDriver) {
      appInfo().name(driverName);
      mClassPathDependencies = parsePomDependencyTree();
    }

    File datagenDir = new File(appInfo().dir(), "dat_files");
    mWithDatagen = datagenDir.exists();
  }

  private static int indexOf(String container, String expr, int start) {
    int position = optIndexOf(container, expr, start);
    if (position < 0)
      throw badArg("Can't find", quote(expr), "within string", quote(container), "starting from", start);
    return position;
  }

  private static int optIndexOf(String container, String expr, int start) {
    return container.indexOf(expr, start);
  }

  private List<DependencyEntry> parsePomDependencyTree() {
    SystemCall s = new SystemCall();
    s.arg("mvn", "dependency:tree", "-DoutputType=dot");
    s.directory(appDir());
    String x = s.systemOut();

    String targ = "digraph \"";
    int c = indexOf(x, targ, 0);
    int a0 = c + targ.length();
    int c0 = indexOf(x, "\"", a0);
    String appExpr = x.substring(a0, c0);
    DependencyEntry appEnt = parseDepEnt(appExpr);

    int c2 = indexOf(x, "}", c);
    String s2 = x.substring(c, c2);

    List<DependencyEntry> deps = arrayList();

    c = 0;
    while (true) {
      targ = "-> \"";
      c2 = optIndexOf(s2, targ, c);
      if (c2 < 0)
        break;
      int c3 = c2 + targ.length();
      int c4 = indexOf(s2, "\"", c3);
      String depExpr = s2.substring(c3, c4);

      deps.add(parseDepEnt(depExpr));
      c = c4;
    }

    List<DependencyEntry> filtered = arrayList();
    filtered.add(appEnt);
    for (DependencyEntry ent : deps) {
      if (ent.phase().equals("compile"))
        filtered.add(ent);
    }
    return filtered;
  }

  private DependencyEntry parseDepEnt(String depExpr) {
    DependencyEntry.Builder b = DependencyEntry.newBuilder();
    List<String> parts = split(depExpr, ':');

    b.group(parts.get(0));
    b.artifact(parts.get(1));
    b.type(parts.get(2));
    b.version(parts.get(3));
    if (parts.size() > 4)
      b.phase(parts.get(4));
    else
      b.phase("compile");

    return b.build();
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

  private void writeTargetIfChanged(String content, boolean executable) {
    File targ = mTargetFile;
    files().mkdirs(Files.parent(targ));
    if (verbose())
      log("writing to:", targ, INDENT, debStr(content));
    files().writeIfChanged(targ, content);
    if (executable)
      files().chmod(targ, 744);
  }

  private void createBuildScript() {
    setTargetWithinProjectAuxDir("make.sh");

    List<String> lines = split(frag("make_template.txt"), '\n');

    List<String> filtered = arrayList();
    boolean state = true;
    int sourceIndex = INIT_INDEX;
    for (String line : lines) {
      sourceIndex++;
      if (line.startsWith("{{")) {
        String arg = line.substring(2);
        switch (arg) {
        default:
          throw badArg("line:", quote(line), "at", 1 + sourceIndex);
        case "":
          state = true;
          break;
        case "driver":
          state = mDriver;
          break;
        case "datagen":
          state = mWithDatagen;
          break;
        }
        continue;
      }
      if (!state)
        continue;
      filtered.add(line);
    }

    String template = String.join("\n", filtered);
    String result = MacroParser.parse(template, macroMap());
    writeTargetIfChanged(result, true);
  }

  private void createBinMk() {
    File binDir = new File(Files.homeDirectory(), "bin");
    if (!binDir.exists()) {
      log("...creating bin directory");
      files().mkdirs(binDir);
    }
    mTargetFile = new File(binDir, "mk");
    String baseMakeText = frag("bin_mk_template.txt");
    writeTargetIfChanged(baseMakeText, true);
  }

  private void setTargetWithinProjectAuxDir(String fname) {
    if (mProjectAuxDir == null) {
      mProjectAuxDir = new File(appDir(), ".jsproject");
      files().mkdirs(mProjectAuxDir);
    }
    mTargetFile = new File(mProjectAuxDir, fname);
  }

  private void createDriver() {
    setTargetWithinProjectAuxDir("driver.sh");
    determineMainClass();
    macroMap().put("run_app_command", constructCommandLine());
    String template = frag("driver_template.txt");
    String result = parseText(template);
    writeTargetIfChanged(result, true);
  }

  private JSMap macroMap() {
    if (mMacroMap == null) {
      JSMap m = map();
      m.put("group_id", "com.jsbase");
      if (mDriver) {
        m.put("app_name", appInfo().name());
      }
      m.put("datagen_gitignore_comment", "# ...add appropriate entries for generated Java files");

      mMacroMap = m;
    }
    return mMacroMap;
  }

  /**
   * Attempt to determine the fully-qualified class name of the driver class
   * (the 'main' method)
   * 
   * If pom file has a driver.class property, use it; otherwise, search the java
   * source files for a main method.
   */
  private void determineMainClass() {
    String mainClass = parsePomProperty("driver.class");
    if (nullOrEmpty(mainClass)) {
      List<String> candidates = arrayList();
      DirWalk w = new DirWalk(new File(appDir(), "src/main/java")).withExtensions("java");
      for (File srcFile : w.files()) {
        String content = Files.readString(srcFile);
        if (content.contains("public static void main(String[] ")) {
          File c = w.rel(srcFile);
          mainClass = chomp(c.toString(), ".java").replace('/', '.');
          candidates.add(mainClass);
        }
      }
      if (candidates.size() > 1) {
        pr("*** Multiple candidates for 'main' class found:", INDENT, candidates);
      } else if (candidates.size() == 1) {
        mainClass = candidates.get(0);
      }
    }
    mMainClass = mainClass;
    log("determined main class:", mMainClass);
  }

  private String constructCommandLine() {
    List<String> args = arrayList();

    args.add("java");
    args.add("-Dfile.encoding=UTF-8");
    args.add("-classpath");
    StringBuilder s = new StringBuilder();
    for (DependencyEntry ent : mClassPathDependencies) {
      if (s.length() > 0)
        s.append(':');
      s.append("$MVN/");
      s.append(ent.group().replace('.', '/'));
      s.append('/');
      s.append(ent.artifact());
      s.append('/');
      s.append(ent.version());
      s.append('/');
      s.append(ent.artifact());
      s.append('-');
      s.append(ent.version());
      s.append(".jar");
    }
    args.add(s.toString());
    args.add(mMainClass);
    args.add("\"$@\"");
    return String.join(" ", args);
  }

  private JSMap mMacroMap;
  private AppInfo.Builder mAppInfo;

  private String mPomContent;
  private Boolean mDriver;
  private Boolean mWithDatagen;
  private List<DependencyEntry> mClassPathDependencies;
  private String mMainClass;
  private File mTargetFile;
  private File mProjectAuxDir;
  private boolean mBinOnlyFlag;

}
