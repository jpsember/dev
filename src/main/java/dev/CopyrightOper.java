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
import java.util.regex.Matcher;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.BasePrinter;
import js.base.DateTimeTools;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.RegExp;

public final class CopyrightOper extends AppOper {

  @Override
  public String userCommand() {
    return "copyright";
  }

  @Override
  public String shortHelp() {
    return "add copyright messages to source files";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("[ sourcedir <dir> |  remove ]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    mSourceDirStr = args.nextArgIf("sourcedir", mSourceDirStr);
    mRemoveFlag = args.nextArgIf("remove");
  }

  @Override
  public void perform() {
    File sourceDir = Files.currentDirectory();
    if (!mSourceDirStr.isEmpty())
      sourceDir = new File(mSourceDirStr);
    checkArgument(sourceDir.isDirectory(), "can't find source directory:", sourceDir);

    String licenseText = readLicense("mit_license.txt");

    DirWalk w = new DirWalk(sourceDir).withExtensions("java");

    for (File sourceFile : w.files()) {
      // Omit 'gen' subdirectories, as we assume this is generated code
      String pathStr = "/" + sourceFile.toString();
      if (pathStr.contains("/gen/"))
        continue;
      log("processing:", sourceFile);
      String orig = Files.readString(sourceFile);

      String result = orig;

      // If there's a license at the start, delete it
      Matcher matcher = RegExp.matcher("\\/\\*\\*(:?\\*|[^\\*\\/][^\\*]*\\*)*\\/\\n*", orig);
      if (matcher.find() && matcher.start() == 0) {
        result = result.substring(matcher.end());
      }

      // This is the regexp from Rubular.com that I was working with:
      //
      // \/\*\*(:?\*|[^\*\/][^\*]*\*)*\/

      // Starts with '/**'
      //
      // has zero or more of one of these:
      //
      //    1. '*'
      //
      //    2. (anything but '*' or '/'), zero or more of (anything but '*'), '*'
      //
      // ends with '/' and zero or more linefeeds
      //

      if (!mRemoveFlag)
        result = licenseText + "\n" + result;

      files().writeIfChanged(sourceFile, result);
    }
  }

  /**
   * 
   * @param resourceName
   * @return
   */
  private String readLicense(String resourceName) {
    String c = frag(resourceName);
    List<String> lines = split("\n" + c + "\n", '\n');

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      String prefix;
      if (i == 0)
        prefix = "/**";
      else if (i == lines.size() - 1)
        prefix = " **/";
      else
        prefix = " * ";
      sb.append(prefix);
      sb.append(lines.get(i));
      sb.append('\n');
    }

    // Look for special year expression, and replace with current year
    String expr = "!!YEAR!!";
    boolean found = true;
    while (found) {
      found = false;
      int cursor = sb.indexOf(expr);
      if (cursor >= 0) {
        found = true;
        int year = DateTimeTools.zonedDateTime(System.currentTimeMillis()).getYear();
        sb.replace(cursor, cursor + expr.length(), "" + year);
      }
    }

    return sb.toString().trim();
  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), "copyright/" + resourceName);
  }

  private String mSourceDirStr = "";
  private Boolean mRemoveFlag = false;

}
