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

import org.junit.Test;

import js.data.DataUtil;
import js.file.Files;
import js.testutil.MyTestCase;

public class CreateAppOperTest extends MyTestCase {

  @Test
  public void sample() {
    addArg("createapp");
    addArg("package", "js.zebra");
    addArg("app_dir", appDir());
    compile();
  }

  @Test
  public void customMain() {
    addArg("createapp");
    addArg("package", "js.zebra");
    addArg("app_dir", appDir());
    addArg("main", "Zebra");
    compile();
  }

  private final void addArg(Object... args) {
    for (Object a : args) {
      mArgs.add(a.toString());
    }
  }

  private File appDir() {
    if (mAppDir == null)
      mAppDir = Files.S.mkdirs(generatedFile(appName()));
    return mAppDir;
  }

  private void runApp() {
    if (verbose())
      addArg("--verbose");
    new Main().startApplication(DataUtil.toStringArray(args()));
    args().clear();
  }

  private void compile() {
    runApp();
    assertGenerated();
  }

  private String appName() {
    return "zebra";
  }

  private List<String> args() {
    return mArgs;
  }

  private File mAppDir;
  private List<String> mArgs = arrayList();

}
