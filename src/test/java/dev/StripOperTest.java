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

import dev.strip.StripOper;
import js.file.DirWalk;
import js.file.Files;
import org.junit.Test;

import java.io.File;

public class StripOperTest extends DevTestBase {

  @Test
  public void strip() {
    prepareApp();
    runApp();
    assertGenerated();
  }


  @Test
  public void explicitFileList() {
    prepareApp();
    // Use an explicit file list
    var fl = "big.rs\n" + "subdir/h2\n";
    var targ = Files.join(mProjectDirSource, ".files");
    files().writeString(targ, fl);
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
    files().writeString(new File(sourceDir(), StripOper.EXPLICIT_FILES_LIST), "subdir");
    files().writeString(new File(sourceDir(), "subdir/" + StripOper.EXPLICIT_FILES_LIST), "c.java\nh2");
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
    path = new File(path, ".delete");
    var newContent = String.join("\n", concatExprs);
    files().writeString(path, newContent);
  }

  private void prepareApp() {
    clearArgs();
    setOper("strip");
    var sourceDir = testFile("project");
    mProjectDirSource = Files.join(generatedDir(), "project");
    files().copyDirectory(sourceDir, mProjectDirSource);
    mProjectDirTarget = Files.join(generatedDir(), "project" + StripOper.TESTING_DIR_SUFFIX);
    files().copyDirectory(sourceDir, mProjectDirTarget);

    // Delete any files that start with 'omitted', '.', or have the extension '.md'
    // from the generated target directory, to
    // simulate the situation where they didn't already exist
    var dw = new DirWalk(mProjectDirTarget).withRecurse(true);
    for (var w : dw.files()) {
      var b = w.getName();
      if (b.startsWith("omitted") || b.endsWith(".md")
          || StripOper.ALWAYS_DELETE_THESE_FILES.contains(b)
      ) {
        files().deleteFile(w);
      }
    }

    addArg("source_branch", "$");
    addArg("target_branch", "$");
    addArg("project_dir", mProjectDirSource);
    addArg("cache_dir", files().mkdirs(cacheDir()));
    addArg("cache_path_expr", "xxx");
    addArg("skip_pattern_search");
  }


  private File sourceDir() {
    return new File(generatedDir(), "project");
  }

  private File cacheDir() {
    return new File(generatedDir(), "cache");
  }

  private File mProjectDirSource;
  private File mProjectDirTarget;

}
