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

import js.app.App;
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
    runApp();
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  @Test
  public void saveAndRestore() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    runApp();

    var f1 =
        generatedFile("source/a.java");
    var f2 =
        generatedFile("source/d.java");

    checkState(f1.exists());
    checkState(!f2.exists());

    prepareApp();
    runApp();
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  @Test
  public void restoreWithoutSave() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    // Delete the project info file within the start directory, so it doesn't assume it's doing a save
    var existingFilter = new File(sourceDir(), PrepOper.PROJECT_INFO_FILE);
    Files.assertExists(existingFilter, "existing project info file for restoreWithoutSave");
    files().deleteFile(existingFilter);
    try {
      runApp();
    } catch (App.AppErrorException e) {
      checkArgument(e.toString().contains("is there a .prep_project file"));
    }
  }

  @Test
  public void performInit() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    prepareInit();
    runApp();

    assertGenerated();
  }

  private void prepareInit() {
    // Delete some things that wouldn't be there if an init is being performed

    var existingFilter = new File(sourceDir(), PrepOper.PROJECT_INFO_FILE);
    files().deleteFile(existingFilter);

    addArg("init");

  }

  @Test
  public void performInitThenSave() {
    prepareDirectories();
    prepareApp();
    prepareInit();
    runApp();

    prepareApp();
    runApp();
    assertGenerated();
  }

  private void prepareApp() {
    loadTools();
    clearArgs();
    setOper("prep");
    addArg("project_root_for_testing", sourceDir());
    addArg("cache_dir", files().mkdirs(cacheDir()));
    addArg("cache_filename", "prep_oper");
    addArg("cache_path_expr", "xxx");
    addArg("skip_pattern_search");
  }


  private void prepareDirectories() {
    var src = testDataDir();
    var target = sourceDir();
    files().mkdirs(target);
    files().copyDirectory(src, target);
  }

  private File sourceDir() {
    return new File(generatedDir(), "source");
  }

  private File cacheDir() {
    return new File(generatedDir(), "cache");
  }

}
