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
import js.file.DirWalk;
import js.file.Files;
import js.json.JSList;
import js.parsing.RegExp;

public final class AppUtil {

  public static File pomFile(File pomDirOrNull) {
    if (Files.empty(pomDirOrNull))
      pomDirOrNull = Files.currentDirectory();
    return new File(pomDirOrNull, "pom.xml");
  }

  public static String readPom(AppInfo appInfo) {
    return Files.readString(appInfo.pomFile());
  }

  public static final Pattern APP_NAME_PATTERN = Pattern.compile("[a-z]+");
  public static final Pattern PACKAGE_PATTERN = Pattern.compile("[a-z]+(\\.[a-z]+)*");

  public static void withDirectory(AppInfo.Builder appInfo, File appDir, String mainClassNameOrNull) {
    checkArgument(Files.nonEmpty(appDir));
    appInfo.dir(appDir);
    String name = appInfo.dir().getName();
    checkArgument(RegExp.patternMatchesString(APP_NAME_PATTERN, name), "Illegal app name:", name);
    appInfo.name(name);
    appInfo.pomFile(new File(appInfo.dir(), "pom.xml"));
    if (appInfo.pomFile().exists()) {
      String pomText = readPom(appInfo);
      String phrase = "<artifactId>";
      int pos = pomText.indexOf(phrase);
      int pos2 = pomText.indexOf("</artifactId>", pos + 1);
      checkArgument(pos >= 0 && pos2 > pos, "can't find " + phrase);
      String appNameFromPom = pomText.substring(pos + phrase.length(), pos2);
      checkArgumentsEqual(appInfo.name(), appNameFromPom, "App name vs pom file");
    }

    appInfo.mainClassName(ifNullOrEmpty(mainClassNameOrNull, "Main"));
    String mainClassName = appInfo.mainClassName() + ".java";
    {
      List<File> mainFiles = arrayList();
      File srcDir = new File(appInfo.dir(), "src/main/java");
      if (srcDir.exists()) {
        for (File f : new DirWalk(srcDir).withExtensions("java").filesRelative()) {
          if (f.getName().equals(mainClassName))
            mainFiles.add(f);
        }
      }
      if (mainFiles.size() > 1)
        badArg("Multiple", mainClassName, "files found:",
            JSList.withStringRepresentationsOf(mainFiles).prettyPrint());

      if (mainFiles.size() == 1) {
        String mainStr = mainFiles.get(0).toString();
        appInfo.mainFile(new File("src/main/java", mainStr));
        appInfo.mainPackage(slashToDot(chomp(mainStr, "/" + mainClassName)));
      }
    }
  }

  public static AppInfo.Builder appInfo(File appDir, String mainClassNameOrNull) {
    AppInfo.Builder appInfo = AppInfo.newBuilder();
    withDirectory(appInfo, appDir, mainClassNameOrNull);
    return appInfo;
  }

  public static String dotToSlash(String str) {
    return str.replace(".", "/");
  }

  public static String slashToDot(String str) {
    return str.replace("/", ".");
  }
}
