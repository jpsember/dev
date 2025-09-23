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
    prepareApp();
    runApp();
    assertGenerated();
  }

  @Test
  public void filterDryRun() {
    prepareApp();
    addArg("--dryrun");
    runApp();
    assertGenerated();
  }

  @Test
  public void saveWithFileList() {
    prepareApp();
    files().writeString(new File(sourceDir(), PrepOper.FILE_LIST_FILENAME), "subdir");
    files().writeString(new File(sourceDir(), "subdir/" + PrepOper.FILE_LIST_FILENAME), "c.java\nh2");
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
    repFilter(relPath, concatExprs);
    prepareApp();
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
    var sourceDir = testFile("project");
    var sourceDirGen = Files.join(generatedDir(), "project");
    files().copyDirectory(sourceDir, sourceDirGen);
    var targetDirGen = Files.join(generatedDir(), "project" + PrepOper.TESTING_DIR_SUFFIX);
    files().copyDirectory(sourceDir, targetDirGen);

    addArg("source_branch", "$");
    addArg("target_branch", "$");
    addArg("project_dir", sourceDirGen);
    addArg("cache_dir", files().mkdirs(cacheDir()));
    addArg("cache_filename", "prep_oper");
    addArg("cache_path_expr", "xxx");
    addArg("skip_pattern_search");
  }


  private File sourceDir() {
    return new File(generatedDir(), "project");
  }

  private File cacheDir() {
    return new File(generatedDir(), "cache");
  }

}
