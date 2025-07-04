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

import static js.base.Tools.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import js.file.Files;

public class CreateAppOperTest extends DevTestBase {

  @Test
  public void zebra() {
    setOper("createapp");
    compile();
  }

  @Test
  public void nameDiffersFromDir() {
    setOper("createapp");
    addArg("name", "foo");
    compile();
  }

  private void compile() {
    // Place the generated directory OUTSIDE of any existing repo, since we will be creating a new repo during the test
    setGeneratedDir(Files.createTempDir("_unit_test_" + name() + "_"));
    provideArg("name", name());
    provideArg("parent_dir", generatedDir());
    var nameDir = argValueForKey("name");
    runApp();
    checkNonEmpty(nameDir, "no 'name' value found", args());
    var gitDir = Files.assertDirectoryExists(new File(generatedDir(), nameDir + "/.git"),
        "generated .git subdir");
    // Delete the .git subdirectory, since its contents are not important (and likely to change frequently)
    files().deleteDirectory(gitDir, ".git");
    assertGenerated();
  }

}
