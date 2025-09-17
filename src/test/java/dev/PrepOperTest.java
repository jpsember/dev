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

import dev.prep.FilterState;
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
   addArg("oper","filter");
    runApp();
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }


  @Test
  public void saveWithFileList() {
    prepareDirectories();

    files().writeString(new File(sourceDir(),PrepOper.FILE_LIST_FILENAME),"subdir");
    files().writeString(new File(sourceDir(),"subdir/"+PrepOper.FILE_LIST_FILENAME),"c.java\nh2");

    // ----------------------------------------------------------------------------------------------
    prepareApp();
    addArg("oper","filter");
    runApp();
    // ----------------------------------------------------------------------------------------------
    assertGenerated();
  }

  @Test
  public void saveAndRestore() {
    prepareDirectories();
    // ----------------------------------------------------------------------------------------------
    prepareApp();
    addArg("oper","filter");
    runApp();

    var f1 =
        generatedFile("source/a.java");
    var f2 =
        generatedFile("source/d.java");

    checkState(f1.exists());
    checkState(!f2.exists());

    prepareApp();
    addArg("oper","restore");
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
      addArg("oper","restore");
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
    addArg("oper","init");

    var existingFilter = new File(sourceDir(), PrepOper.PROJECT_INFO_FILE);
    files().deleteFile(existingFilter);

//    addArg("init");

  }

  @Test
  public void performInitThenSave() {
    prepareDirectories();
    prepareApp();
    prepareInit();
    runApp();

    prepareApp();
    addArg("oper","filter");
    runApp();
    assertGenerated();
  }


  @Test
  public void filterPaths() {
    auxFilt("", "d.java", "subdir/j2");
  }


  @Test
  public void filterFileAndSubdir() {
    auxFilt("", "d.java", "subdir/j2");
  }

  @Test
  public void pathValidator() {
    var m = map();
    String[] exp = {"d.java", "a/b/c.txt", "a/b/.c/.d", "ab..cd", "./abc/def", ".abc/.def", "abc def/alpha", "al\\b/e", "abc/", "/def", "abc//def"};
    for (var s : exp) {
      var result = FilterState.isValidFilename(s);
      m.putNumbered(s, result ? "ok" : "*** PROBLEM");
    }
    assertMessage(m);
  }

  private void auxFilt(String relPath, String... concatExprs) {
    prepareDirectories();
    repFilter(relPath, concatExprs);
    prepareApp();
    addArg("oper","filter");
    runApp();
    assertGenerated();
  }

  private void repFilter(String relPath, String... concatExprs) {
    var path = sourceDir();
    if (!relPath.isEmpty()) {
      path = new File(path, relPath);
    }
    path = new File(path, ".filter");
    var newContent = String.join("\n", concatExprs);
    files().writeString(path, newContent);
  }

  private void prepareApp() {
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
