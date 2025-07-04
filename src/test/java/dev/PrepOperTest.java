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

import dev.gen.PrepConfig;
import js.data.DataUtil;
import js.file.Files;
import js.testutil.MyTestCase;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static js.base.Tools.arrayList;
import static js.base.Tools.checkNonEmpty;
import static js.base.Tools.*;
public class PrepOperTest extends MyTestCase {

  @Test
  public void oper() {
    doit();
  }

  private void doit() {
    var src = testDataDir();
    var target = generatedDir();
    files().copyDirectory(src,target);

    todo("refactor to make addArg, etc part of a DevOperTestCase class");

    addArg("prep");
    addArg("save");
    addArg("dir", src);

    runApp();
    assertGenerated();
  }
  private final void addArg(Object... args) {
    for (Object a : args) {
      mArgs.add(a.toString());
    }
  }

  private void runApp() {
    if (verbose())
      addArg("--verbose");
    new Main().startApplication(DataUtil.toStringArray(args()));
    args().clear();
  }

  private List<String> args() {
    return mArgs;
  }

  private List<String> mArgs = arrayList();


}
