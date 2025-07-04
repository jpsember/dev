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
 **/
package dev;

import js.data.DataUtil;
import js.testutil.MyTestCase;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static js.base.Tools.*;

public class PrepOperTest extends MyTestCase {

  @Test
  public void save() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    todo("refactor to make addArg, etc part of a DevOperTestCase class");
    perform("save");
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  @Test
  public void saveAndRestore() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    todo("refactor to make addArg, etc part of a DevOperTestCase class");
    perform("save");
    prepareApp();
    perform("restore");
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  private void prepareApp() {
    clearArgs();
    addArg("prep");
    addArg("dir", sourceDir());
    addArg("project_file", "_proj_file_");
    addArg("cache_dir", files().mkdirs(cacheDir()));
    addArg("cache_filename","prep_oper");
    addArg("cache_path_expr","xxx");
  }

//  private void doit() {
//    prepareDirectories();
//    prepareApp();
//    todo("refactor to make addArg, etc part of a DevOperTestCase class");
//    perform("save");
////
////    addArg("save");
////    addArg("dir", target);
////    addArg("project_file", "_proj_file_");
//
////    runApp();
//    assertGenerated();
//  }

  private void prepareDirectories() {
    var src = testDataDir();
    var target = sourceDir();
    files().mkdirs(target);
    files().copyDirectory(src, target);
  }

  private void perform(String action) {
    addArg(action);
//    var src = testDataDir();
//    var target = generatedDir();
//    files().copyDirectory(src, target);
    runApp();
  }

  private void addArg(Object... args) {
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

  private File sourceDir() {
    return new File(generatedDir(), "source");
  }

  private File cacheDir() {
    return new File(generatedDir(), "cache");
  }

  private void clearArgs() {
    mArgs.clear();
  }

  private List<String> args() {
    return mArgs;
  }

  private List<String> mArgs = arrayList();

}
