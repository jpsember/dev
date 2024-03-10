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

import dev.gen.AppInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.BasePrinter;
import js.file.DirWalk;
import js.file.Files;

public class ResetTestOper extends AppOper {

  @Override
  public String userCommand() {
    return "resettest";
  }

  @Override
  public String getHelpDescription() {
    return "Discard all unit test hash codes and temporary files";
  }

 
  @Override
  protected void getOperSpecificHelp(BasePrinter b) {
    b.pr("[startdir <dir>]*");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    mStartDirString = args.nextArgIf("startdir", mStartDirString);
  }

  @Override
  public void perform() {
    if (mStartDirString.isEmpty())
      mStartDir = Files.currentDirectory();
    else
      mStartDir = new File(mStartDirString);

    AppInfo appInfo = AppUtil.appInfo(mStartDir, null);
    log("Resetting tests for app:", INDENT, appInfo);
    File unitTestDir = new File(appInfo.dir(), "unit_test");
    for (File hashFile : new DirWalk(unitTestDir).withRecurse(false).withExtensions(Files.EXT_JSON).files()) {
      files().deleteFile(hashFile);
    }
    files().deleteDirectory(new File(mStartDir, "_SKIP_generated"));
    files().deleteDirectory(new File(mStartDir, "unit_test/generated"));
  }

  private String mStartDirString = "";
  private File mStartDir;

}
