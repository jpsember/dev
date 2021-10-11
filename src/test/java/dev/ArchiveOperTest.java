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
import js.app.App;
import js.data.DataUtil;
import js.file.Files;
import js.json.JSMap;
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

  /**
   * Push new versions of a couple of items
   */
  @Test
  public void pushUpdate() {
    prepareWorkCopies();

    addArg("push", "alpha");
    runApp();

    addArg("push", "delta.txt");
    runApp();

    runApp();

    assertGenerated();
  }

  @Test
  public void validateBadVersion() {
    validate(map().put("version", "foo"));
  }

  @Test
  public void validatePathNotRelative() {
    validate(map().put("entries", map().put("a", map().put("path", "/abs/path/not/allowed"))));
  }

  @Test
  public void validatePathIllegalChars() {
    validate(map().put("entries", map().put("a", map().put("path", "illegal/chars-in-path"))));
  }

  @Test
  public void validatePathBadFilename() {
    validate(map().put("entries", map().put("a", map().put("path", "illegal/path.repeated.extensions"))));
  }

  @Test
  public void validatePathIllformed() {
    validate(map().put("entries", map().put("a", map().put("path", "bad//path"))));
  }

  @Test
  public void validateBadVersionHidden() {
    validate(null, map().put("version", "foo"));
  }

  // ------------------------------------------------------------------

  private final void addArg(Object... args) {
    for (Object a : args) {
      args().add(a.toString());
    }
  }

  private void runApp() {
    addArg("dir", mWorkLocal);
    addArg("mock_remote", mWorkRemote);
    App app = new Main();
    app.startApplication(DataUtil.toStringArray(args()));
    mArgs = null;
    RuntimeException e = app.getError();
    if (e != null)
      Files.S.writeString(generatedFile("_error_.txt"), e.toString());
  }

  private void execute() {
    prepareWorkCopies();
    runApp();
    assertGenerated();
  }

  private void prepareWorkCopies() {
    // Create copies of the local and remote directories (where they exist)
    // so that we only modify the copies during the unit test 

    File unitTestSourceData = new File(testDataDir(), name());

    File templateLocal = new File(unitTestSourceData, "local");
    File templateRemote = new File(unitTestSourceData, "remote");
    mWorkLocal = generatedFile("local");
    mWorkRemote = generatedFile("remote");
    Files.S.copyDirectory(templateLocal, mWorkLocal);
    if (templateRemote.exists())
      Files.S.copyDirectory(templateRemote, mWorkRemote);
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

  private void validate(JSMap registry, JSMap hiddenRegistry) {
    if (registry == null)
      registry = map();

    mWorkLocal = generatedFile("local");
    mWorkRemote = generatedFile("remote");
    Files.S.writePretty(new File(mWorkLocal, "archive_registry.json"), registry);
    if (hiddenRegistry != null)
      Files.S.writePretty(new File(mWorkLocal, ".archive_registry.json"), hiddenRegistry);

    addArg("validate");
    runApp();
    assertGenerated();
  }

  private void validate(JSMap registry) {
    validate(registry, null);
  }

  private List<String> mArgs;
  private File mWorkLocal;
  private File mWorkRemote;

}
