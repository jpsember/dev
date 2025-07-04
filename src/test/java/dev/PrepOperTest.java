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

import js.file.Files;
import org.junit.Test;

import java.io.File;

import static js.base.Tools.*;

public class PrepOperTest extends DevTestBase {

  @Test
  public void save() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    perform("save");
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  @Test
  public void saveAndRestore() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    perform("save");

    var f1 =
        generatedFile("source/a.java");
    var f2 =
        generatedFile("source/d.java");

    checkState(f1.exists());
    checkState(!f2.exists());

    prepareApp();
    perform("restore");
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  private void prepareApp() {
    loadTools();
    clearArgs();
    setOper("prep");
    addArg("dir", sourceDir());
    addArg("project_file", "_proj_file_");
    addArg("cache_dir", files().mkdirs(cacheDir()));
    addArg("cache_filename", "prep_oper");
    addArg("cache_path_expr", "xxx");
  }


  private void prepareDirectories() {
    var src = testDataDir();
    var target = sourceDir();
    files().mkdirs(target);
    files().copyDirectory(src, target);
  }

  private void perform(String action) {
    addArg(action);
    runApp();
  }

  private File sourceDir() {
    return new File(generatedDir(), "source");
  }

  private File cacheDir() {
    return new File(generatedDir(), "cache");
  }

}
