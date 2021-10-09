/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
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

import dev.Main;
import js.data.DataUtil;
import js.file.Files;
import js.testutil.MyTestCase;

public class ArchiveOperTest extends MyTestCase {

  /**
   * Pushes initial versions of some objects, since they have entries within the
   * (global) registry but no version numbers
   */
  @Test
  public void pushInitialVersions() {
    execute();
  }

  /**
   * Mark an object for forgetting
   */
  @Test
  public void forgetMark() {
    addArg("forget", "alpha");
    execute();
  }

  /**
   * Forget an object, erasing it from the global (and local) registries
   */
  @Test
  public void forgotten() {
    execute();
  }

  // ------------------------------------------------------------------

  private final void addArg(Object... args) {
    for (Object a : args) {
      args().add(a.toString());
    }
  }

  private void runApp() {
    new Main().startApplication(DataUtil.toStringArray(args()));
    args().clear();
  }

  private void execute() {

    // Create copies of the local and remote directories (where they exist)
    // so that we only modify the copies during the unit test 

    File unitTestSourceData = new File(testDataDir(), name());

    File templateLocal = new File(unitTestSourceData, "local");
    File templateRemote = new File(unitTestSourceData, "remote");
    File workLocal = generatedFile("local");
    File workRemote = generatedFile("remote");
    Files.S.copyDirectory(templateLocal, workLocal);
    if (templateRemote.exists())
      Files.S.copyDirectory(templateRemote, workRemote);

    addArg("dir", workLocal);
    addArg("mock_remote", workRemote);

    runApp();
    assertGenerated();
  }

  private List<String> args() {
    if (mArgs == null) {
      mArgs = arrayList();
      addArg("archive");
      if (verbose())
        addArg("--verbose");
    }
    return mArgs;
  }

  private List<String> mArgs;

}
