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
import dev.gen.archive.ArchiveEntry;
import dev.gen.archive.ArchiveRegistry;
import dev.gen.archive.LocalEntry;
import dev.gen.archive.LocalRegistry;
import dev.gen.archive.Oper;
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
    generateFiles(workLocal(),
        "alpha(beta.txt) epsilon(hotel(f1.txt f2.txt)) golf(yankee(f1.txt f2.txt)) gamma.txt");

    global();
    flushEnt("!alpha");
    path("epsilon/hotel").flushEnt("!hotel");
    path("golf/yankee").flushEnt("!zulu");
    flushEnt("gamma.txt");
    flushRegistry();

    addArg("update");
    runApp();
    assertGenerated();
  }

  /**
   * Push an object that is not already in the archive, by referring to its path
   */
  @Test
  public void pushViaPath() {
    generateFiles(workLocal(),
        "alpha(beta.txt) epsilon(hotel(f1.txt f2.txt)) golf(yankee(f1.txt f2.txt)) gamma.txt");

    global();
    flushRegistry();

    // First call is to mark item for pushing
    //
    addArg("push", relative("epsilon/hotel"));
    runApp();

    // Second is to actually perform the pushing
    addArg("update");
    runApp();

    assertGenerated();
  }

  /**
   * Fail pushing a new object via a path that conflicts with an existing
   * object's key
   */
  @Test
  public void pushViaPathConflict() {
    generateFiles(workLocal(),
        "alpha(beta.txt) epsilon(hotel(f1.txt f2.txt)) golf(yankee(f1.txt f2.txt)) gamma.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).path("gamma.txt").flushEnt("hotel");
    vers(1).path("golf/yankee").flushEnt("!zulu");
    flushRegistry();

    local();
    vers(1).flushEnt("alpha");
    vers(1).flushEnt("hotel");
    vers(1).flushEnt("zulu");
    flushRegistry();

    addArg("push", relative("epsilon/hotel"));
    runApp();

    assertGenerated();
  }

  /**
   * Mark an object for forgetting
   */
  @Test
  public void forgetMark() {
    generateFiles(workLocal(), "alpha(beta.txt) gamma.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("gamma.txt");
    flushRegistry();

    local();
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("gamma.txt");
    flushRegistry();

    addArg("forget", "alpha");
    runApp();
    assertGenerated();
  }

  /**
   * Perform a pending forget, erasing it from the global (and local) registries
   */
  @Test
  public void forgetPerform() {
    generateFiles(workLocal(), "alpha(beta.txt) gamma.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("gamma.txt");
    flushRegistry();

    local();
    vers(1).pending(Oper.FORGET).flushEnt("!alpha");
    vers(1).flushEnt("gamma.txt");
    flushRegistry();

    addArg("update");
    runApp();
    assertGenerated();
  }

  /**
   * Mark a directory for offloading
   */
  @Test
  public void offloadMark() {
    generateFiles(workLocal(), "alpha(beta.txt) delta.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).path("delta.txt").flushEnt("delta");
    flushRegistry();

    local();
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("delta");
    flushRegistry();

    addArg("offload", "alpha");

    runApp();

    assertGenerated();
  }

  /**
   * Verify that a new version of an offloaded object is not pulled
   */
  @Test
  public void offloadWithOldVersion() {
    generateFiles(workLocal(), "alpha(beta.txt) delta.txt");

    global();
    vers(5).flushEnt("!alpha");
    vers(3).path("delta.txt").flushEnt("delta");
    flushRegistry();

    local();
    vers(2).offload().flushEnt("alpha");
    vers(1).offload().flushEnt("delta");
    flushRegistry();

    addArg("update");

    runApp();

    assertGenerated();
  }

  /**
   * Perform offloading
   */
  @Test
  public void offloadPerform() {
    generateFiles(workLocal(), "alpha(beta.txt) delta.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).path("delta.txt").flushEnt("delta");
    flushRegistry();

    local();
    pending(Oper.OFFLOAD);
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("delta");
    flushRegistry();

    addArg("update");
    runApp();

    assertGenerated();
  }

  /**
   * Push new versions of a couple of items
   */
  @Test
  public void pushUpdate() {
    generateFiles(workLocal(), "alpha(beta.txt) epsilon.txt delta.txt");

    global();
    vers(1).flushEnt("!alpha");
    vers(1).path("epsilon.txt").flushEnt("epsilon");
    vers(1).path("delta.txt").flushEnt("delta");
    flushRegistry();

    local();
    vers(1).flushEnt("!alpha");
    vers(1).flushEnt("epsilon");
    vers(1).flushEnt("delta");
    flushRegistry();

    addArg("push", "alpha");
    runApp();

    addArg("push", relative("delta.txt"));
    runApp();

    // Run to actually perform the push
    addArg("update");
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
    validate(map().put("entries", map().put("a", map().put("path", "illegal/path..strangesequence"))));
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
    addArg("dir", workLocal());
    addArg("mock_remote", workRemote());
    App app = new Main();
    app.setFiles(files());
    app.startApplication(DataUtil.toStringArray(args()));
    mArgs = null;
    RuntimeException e = app.getError();
    if (e != null)
      files().writeString(generatedFile("_error_.txt"), e.toString());
  }

  /**
   * If not already done, set the project directory to the generated 'local'
   * directory
   */
  private void prepareProject() {
    if (!mProjectPrepared) {
      mProjectPrepared = true;
      files().setProjectDirectory(workLocal());
    }
  }

  private boolean mProjectPrepared;

  private List<String> args() {
    if (mArgs == null) {
      mArgs = arrayList();
      addArg("archive");
      if (verbose())
        addArg("--verbose");
    }
    return mArgs;
  }

  /**
   * Get generated 'local' directory
   */
  private File workLocal() {
    if (mWorkLocal == null) {
      prepareProject();
      mWorkLocal = generatedFile("local");
    }
    return mWorkLocal;
  }

  /**
   * Get generated 'remote' directory
   */
  private File workRemote() {
    if (mWorkRemote == null) {
      mWorkRemote = generatedFile("remote");
    }
    return mWorkRemote;
  }

  private void validate(JSMap registry, JSMap hiddenRegistry) {
    generateFiles(workLocal(), "");

    if (registry == null)
      registry = map();

    File configDir = new File(workLocal(), "project_config");
    files().mkdirs(configDir);
    files().writePretty(new File(configDir, "archive_registry.json"), registry);
    if (hiddenRegistry != null)
      files().writePretty(new File(configDir, ".archive_registry.json"), hiddenRegistry);

    addArg("update");
    runApp();
    assertGenerated();
  }

  private void validate(JSMap registry) {
    validate(registry, null);
  }

  /**
   * Modify file so that it is relative to the current directory
   */
  private File relative(String path) {
    File placedWithinWorkLocal = new File(workLocal(), path);
    return Files.fileRelativeToDirectory(placedWithinWorkLocal, Files.currentDirectory());
  }

  private void generateFiles(File startDirectory, String script) {
    mCursor = 0;
    mScript = script;
    mParent = startDirectory;
    mParentStack = arrayList();
    genHelper();

  }

  // ------------------------------------------------------------------
  // Generating an ArchiveRegistry
  // ------------------------------------------------------------------

  private ArchiveEntry.Builder ent() {
    if (mArchiveEntryBuilder == null) {
      mArchiveEntryBuilder = ArchiveEntry.newBuilder();
    }
    return mArchiveEntryBuilder;
  }

  private LocalEntry.Builder localEnt() {
    if (mLocalEntryBuilder == null) {
      mLocalEntryBuilder = LocalEntry.newBuilder();
    }
    return mLocalEntryBuilder;
  }

  private ArchiveRegistry.Builder reg() {
    if (mGlobalRegistry == null)
      mGlobalRegistry = ArchiveRegistry.newBuilder();
    return mGlobalRegistry;
  }

  private LocalRegistry.Builder localReg() {
    if (mLocalRegistry == null)
      mLocalRegistry = LocalRegistry.newBuilder();
    return mLocalRegistry;
  }

  /**
   * Make subsequent operations operate on the global registry
   */
  private ArchiveOperTest global() {
    mRegIndex = 1;
    return this;
  }

  /**
   * Make subsequent operations operate on the local registry
   */
  private ArchiveOperTest local() {
    mRegIndex = 2;
    return this;
  }

  private boolean isGlobal() {
    checkState(mRegIndex != 0);
    return mRegIndex == 1;
  }

  /**
   * Set the path field (must be global registry)
   */
  private ArchiveOperTest path(String path) {
    checkState(isGlobal());
    ent().path(new File(path));
    return this;
  }

  /**
   * Set version (global or local)
   */
  private ArchiveOperTest vers(int version) {
    if (isGlobal())
      ent().version(version);
    else
      localEnt().version(version);
    return this;
  }

  /**
   * Set pending operation (local registry)
   */
  private ArchiveOperTest pending(Oper oper) {
    checkState(!isGlobal());
    localEnt().pending(oper);
    return this;
  }

  /**
   * Set offload status
   */
  private ArchiveOperTest offload() {
    checkState(!isGlobal());
    localEnt().offload(true);
    return this;
  }

  /**
   * Flush the current entry (global or local)
   * 
   * @param key
   *          key for entry; prefix '!' indicates that the entry is a directory
   */
  private void flushEnt(String key) {
    if (isGlobal()) {
      boolean isDir = key.startsWith("!");
      key = chompPrefix(key, "!");
      if (isDir)
        ent().directory(true);
      reg().entries().put(key, ent().build());
      mArchiveEntryBuilder = null;
    } else {
      key = chompPrefix(key, "!");
      localReg().entries().put(key, localEnt().build());
      mLocalEntryBuilder = null;
    }
  }

  /**
   * Flush current registry (global or local)
   */
  private void flushRegistry() {
    if (isGlobal()) {
      checkState(mArchiveEntryBuilder == null, "entry not flushed:", mArchiveEntryBuilder);
      ArchiveRegistry reg = reg().build();
      File configDir = new File(workLocal(), "project_config");
      files().mkdirs(configDir);
      files().writePretty(new File(configDir, "archive_registry.json"), reg);
      mGlobalRegistry = null;
    } else {
      checkState(mLocalEntryBuilder == null, "entry not flushed:", mLocalEntryBuilder);
      LocalRegistry reg = localReg().build();
      File configDir = new File(workLocal(), "project_config");
      files().mkdirs(configDir);
      files().writePretty(new File(configDir, ".archive_registry.json"), reg);
      mLocalRegistry = null;
    }
  }

  // Which registry is being operated on (0:none; 1:global; 2:local)
  private int mRegIndex;
  private ArchiveEntry.Builder mArchiveEntryBuilder;
  private ArchiveRegistry.Builder mGlobalRegistry;
  private LocalEntry.Builder mLocalEntryBuilder;
  private LocalRegistry.Builder mLocalRegistry;

  // ------------------------------------------------------------------
  // Generating a directory via a script
  // ------------------------------------------------------------------

  private boolean done() {
    return mCursor == mScript.length();
  }

  private char peek() {
    if (done())
      return 0;
    return mScript.charAt(mCursor);
  }

  private char readChar() {
    return mScript.charAt(mCursor++);
  }

  private void skipWhitespace() {
    while (!done() && peek() <= ' ')
      mCursor++;
  }

  private boolean isFilenameChar(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || ".".indexOf(c) >= 0;
  }

  private void readChar(char ch) {
    if (peek() != ch)
      scriptError();
    readChar();
  }

  private void scriptError() {
    badArg("unexpected char:", mScript.substring(0, mCursor), ">", mScript.substring(mCursor));
  }

  private void genHelper() {
    while (true) {
      skipWhitespace();
      char c = peek();
      if (c == 0 || c == ')')
        return;

      if (!isFilenameChar(c))
        scriptError();
      int i = mCursor;
      int j = i;
      while (isFilenameChar(peek())) {
        readChar();
        j++;
      }
      String fname = mScript.substring(i, j);
      File nextFile = new File(mParent, fname);

      skipWhitespace();
      if (peek() == '(') {
        // process a directory
        mParentStack.add(mParent);
        mParent = nextFile;
        files().mkdirs(mParent);
        readChar('(');
        genHelper();
        readChar(')');
        mParent = pop(mParentStack);
      } else {
        // create a file
        File flatFile = nextFile;
        files().writeString(flatFile, "This is " + fname);
      }
      skipWhitespace();
    }
  }

  private String mScript;
  private int mCursor;
  private File mParent;
  private List<File> mParentStack;

  // ------------------------------------------------------------------

  private List<String> mArgs;
  private File mWorkLocal;
  private File mWorkRemote;
}
