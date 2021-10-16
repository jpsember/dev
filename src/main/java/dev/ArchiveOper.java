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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import js.file.DirWalk;
import js.file.Files;
import js.app.AppOper;
import js.base.BasePrinter;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;
import dev.gen.archive.ArchiveEntry;
import dev.gen.archive.ArchiveRegistry;

/**
 * 
 * This is a tool for saving large objects to an external data store, e.g.,
 * Amazon Web Service's S3.
 * 
 * Each object is either an individual file, or a zipped file directory. These
 * objects have version numbers, so a newer version of an object can be pushed
 * to the data store, where it retains the most recent several versions.
 * 
 * It is designed to serve as a more practical alternative to storing such
 * objects in git, especially if the objects are not going to change frequently.
 * 
 * Relevant variables and files:
 * 
 * <pre>
 * 
 *  project root directory
 *    Object paths are given relative to this directory.  Also the directory containing
 *    the archive registry (this may change in future).
 *    
 *  archive_registry.json
 *    Represents the state of each object within the external data store, including its relative path, its version number,
 *    and some optional flags. Each object record has a unique key (independent of its path). This is tracked by git.
 *    
 *  .archive_registry.json
 *    Records the state of each object within the local machine.  If the local version of an object is
 *    older than that within the external data store (or if no local version exists), then the object must be
 *    copied from the external store.  Not tracked by git.
 *  
 *  AWS S3 credentials
 *    More information to follow.
 * 
 * </pre>
 * 
 * Note that no attempt is made to deal with conflicts that will arise if two
 * separate machines attempt to store new versions of the same object to the
 * external data store simultaneously. By contrast, if they independently modify
 * different objects, then git ought to be able to merge the resulting changes
 * to the archive_registry.json file without difficulty.
 *
 * <pre>
 * 
 * Operations
 * ======================================================================================================
 * 
 * After these operations, it may be necessary to perform a git commit, so that changes to
 * archive_registry.json are propagated to other machines.
 * 
 * 
 * Adding an object to the archive
 * ------------------------------------------------------------------------------------
 * 
 * Suppose you wish to add the file (or directory) "<project root directory>/abc/xyz/foo" to the archive.
 * 
 *   dev archive push <path to/>foo
 *   
 * In the above example, the relative path is the key.  If this creates a conflict, or some other key is desired,
 * then (at present) you must edit archive_registry.json, inserting a key/value pair with the desired key (e.g. "moo"):
 *  
 *        "moo" : { "path" : "abc/xyz/foo",
 *                  "directory" : true
 *                }
 *        
 * Run the archive operation to push the file to the archive:
 * 
 *    dev archive update
 * 
 * Additional fields can be added to the map above:
 * 
 *    TODO: document these fields, e.g. "file_extensions", "offload"
 * 
 * 
 * Pushing a new version of an object to the archive
 * ------------------------------------------------------------------------------------
 * Suppose you have modified "<project root directory>/abc/xyz/foo" and want to push the new version to the
 * archive.
 * 
 * Run the archive operation with these arguments:
 * 
 *    dev archive push foo
 *    dev archive update
 *    
 * The first line registers the object with the local registry.  The second call actually performs the pushing
 * to the archive.  Multiple files can be added before a final "dev archive" call is made.
 *    
 * Alternatively, if the object has a key that is different from its path (e.g. "moo"):
 * 
 *    dev archive push moo
 *    dev archive update
 *    
 * 
 * Removing an object from the archive
 * ------------------------------------------------------------------------------------
 * To make the archive stop tracking "<project root directory>/abc/xyz/foo":
 * 
 *    dev archive forget foo        (or "moo", if that is the key)
 *    dev archive update
 *    
 * 
 * Note that neither the local copy of an object, nor any previously pushed versions in the external data store,
 * are deleted by this operation.  It only makes the archive "forget" about the object.  It must be manually deleted
 * (if desired) from the local machine or the data store.
 * 
 * 
 * Offloading the local copy of an object
 * ------------------------------------------------------------------------------------
 * You can avoid having an archived object being stored locally.  This will delete the existing local copy of the
 * object and will avoid pulling it in from the external store in subsequent operations.
 * 
 *   dev archive offload foo
 * 
 * 
 * </pre>
 * 
 */
public final class ArchiveOper extends AppOper {

  @Override
  public String userCommand() {
    return "archive";
  }

  @Override
  public String getHelpDescription() {
    return "synchronize local project files with cloud archive";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList(//
        "[dir <path>] : project root directory", CR, //
        "[mock_remote <path>] : directory simulating cloud archive device", CR, //
        "[push <path>] : mark file or directory for pushing new version", CR, //
        "[forget <path>] : stop tracking file or directory within archive", CR, //
        "[offload <path>] : delete local copy of object", CR, //
        "[validate] : perform validation only (for test purposes)");
  }

  @Override
  protected void processAdditionalArgs() {
    mProjectDirectory = new File(cmdLineArgs().nextArgIf("dir", ""));
    mMockRemoteDir = new File(cmdLineArgs().nextArgIf("mock_remote", ""));
    mPushPathArg = cmdLineArgs().nextArgIf("push", "");
    mForgetPathArg = cmdLineArgs().nextArgIf("forget", "");
    mOffloadPathArg = cmdLineArgs().nextArgIf("offload", "");
    mValidateOnly = cmdLineArgs().nextArgIf("validate");
    mPerformFlag = cmdLineArgs().nextArgIf("perform");
  }

  private void fixPaths() {
    if (Files.empty(mProjectDirectory))
      mProjectDirectory = Files.getCanonicalFile(Files.parent(files().projectConfigDirectory()));
    else
      mProjectDirectory = Files.getCanonicalFile(mProjectDirectory);
    if (Files.nonEmpty(mMockRemoteDir))
      mMockRemoteDir = Files.getCanonicalFile(mMockRemoteDir);
    mTemporaryFile = fileWithinProjectDir("_SKIP_temp.zip");
  }

  enum Oper {
    PERFORM, PUSH, FORGET, OFFLOAD
  };

  private Oper mOper;

  private void setOper(Oper oper, boolean flag) {
    if (flag) {
      auxSetOper(oper);
    }
  }

  private void setOper(Oper oper, String arg) {
    setOper(oper, nonEmpty(arg));
  }

  private void auxSetOper(Oper oper) {
    if (mOper != null && mOper != oper)
      setError("Multiple operations selected:", mOper, oper);
    mOper = oper;
  }

  @Override
  public void perform() {
    {
      setOper(Oper.PERFORM, mPerformFlag);
      setOper(Oper.PUSH, mPushPathArg);
      setOper(Oper.FORGET, mForgetPathArg);
      setOper(Oper.OFFLOAD, mOffloadPathArg);

      if (mOper == null) {
        alert("Assuming 'perform' was desired; in future, this will be mandatory");
        setOper(Oper.PERFORM, true);
      }
    }
    auxPerform();
    flushRegistries();
  }

  private void auxPerform() {

    // TODO: when error occurs, does registry need to be flushed?  do we care?

    fixPaths();

    readGlobalRegistry();
    readHiddenRegistry();

    if (mValidateOnly) {
      //die("no longer supported");
      return;
    }

    switch (mOper) {
    case PUSH:
      markForPushing(mPushPathArg);
      break;
    case FORGET:
      markForForgetting(mForgetPathArg);
      break;
    case OFFLOAD:
      markForOffloading(mOffloadPathArg);
      break;
    case PERFORM: {
      processForgetFlags();
      updateEntries();
      flushRegistries();
      log(map().put("entries", mRegistryGlobal.entries().size())//
          .put("pushed", mPushedCount)//
          .put("pulled", mPulledCount)//
          .put("offloaded", mOffloadedCount)//
          .put("forgotten", mForgottenCount)//
      );
    }
      break;
    }
  }

  private static final Pattern RELATIVE_PATH_PATTERN = RegExp
      .pattern("^\\.?\\w+(?:\\/\\.?\\w+)*(?:\\.\\w+)*$");

  private String contextExpr(Object contextOrNull) {
    return "(" + nullTo(contextOrNull, "no context given") + ")";
  }

  private void validateGlobalRegistry(ArchiveRegistry registry, Object context) {
    BasePrinter p = new BasePrinter();

    String expected = ArchiveRegistry.DEFAULT_INSTANCE.version();
    if (!registry.version().equals(expected))
      p.pr("*** Bad version number:", registry.version(), "; expected", expected);
    else {
      for (Entry<String, ArchiveEntry> entry : registry.entries().entrySet()) {
        String key = entry.getKey();
        ArchiveEntry ent = entry.getValue();
        File pt = ent.path();
        String problemText = null;
        if (!RegExp.patternMatchesString(RELATIVE_PATH_PATTERN, pt.toString()))
          problemText = "Illegal path";
        else {
          if (ent.version() != 0 || ent.directory() == Boolean.TRUE) {
            File actualFile = fileWithinProjectDir(pt);
            if (actualFile.exists() && actualFile.isDirectory() != (ent.directory() == Boolean.TRUE))
              problemText = "Directory flag is incorrect";
          }
        }
        if (nonEmpty(problemText))
          p.pr("***", problemText, "; key:", key, INDENT, ent, OUTDENT);
      }
    }

    String errorMessage = p.toString();
    if (nonEmpty(errorMessage))
      setError("Problems with archive registry", contextExpr(context), ":", INDENT, errorMessage);
  }

  private void readGlobalRegistry() {
    File globalFile = registerGlobalFile();
    ArchiveRegistry registry = Files.parseAbstractData(ArchiveRegistry.DEFAULT_INSTANCE, globalFile);
    mRegistryGlobalOriginal = registry;
    registry = updateGlobalRegistry(registry);
    validateGlobalRegistry(registry, globalFile.getName());
    mRegistryGlobal = registry.toBuilder();
  }

  private File registerGlobalFile() {
    return fileWithinConfigDir("archive_registry.json");
  }

  private File registerLocalFile() {
    return fileWithinConfigDir(".archive_registry.json");
  }

  private File fileWithinConfigDir(String path) {
    return new File(files().projectConfigDirectory(), Files.assertRelative(path));
  }

  private void readHiddenRegistry() {
    File hiddenFile = registerLocalFile();
    ArchiveRegistry registry = Files.parseAbstractDataOpt(ArchiveRegistry.DEFAULT_INSTANCE, hiddenFile);
    mRegistryLocalOriginal = registry;
    registry = updateHiddenRegistry(registry, mRegistryGlobal);
    mRegistryLocal = registry.toBuilder();
  }

  private ArchiveRegistry updateGlobalRegistry(ArchiveRegistry registry) {
    boolean v1Update = registry.version().equals("1.0");
    ArchiveRegistry.Builder b = registry.build().toBuilder();

    // Replace keys with basenames, and set path to key, where appropriate

    for (Entry<String, ArchiveEntry> ent : registry.entries().entrySet()) {
      String key = ent.getKey();
      ArchiveEntry entry = ent.getValue();
      ArchiveEntry.Builder newEntry = entry.toBuilder();

      String newKey = key;
      if (v1Update) {
        String basename = Files.basename(key);
        if (!basename.equals(key)) {
          // change the key, if possible.  Remove existing mapping, verify no mapping exists for new key, and point new key at mapping
          ArchiveEntry currentMapping = b.entries().get(basename);
          if (currentMapping != null) {
            setError("cannot rename key", key, "to", basename, "as that key already points to:", INDENT,
                currentMapping);
          }
          newKey = basename;
          b.entries().remove(key);
        }
      }

      // If path is empty, set it equal to the key
      // (user may not have specified a path when adding an object manually; especially in unit test data)
      //
      if (Files.empty(entry.path()))
        newEntry.path(new File(key));

      if (v1Update) {
        // Initialize the directory flag
        File absPath = fileWithinProjectDir(newEntry.path());
        if (absPath.isDirectory())
          newEntry.directory(true);
      }

      if (newEntry.fileExtensions() != null && newEntry.fileExtensions().isEmpty())
        newEntry.fileExtensions(null);

      // Discard optional boolean values if they are false
      //
      if (newEntry.forget() != Boolean.TRUE)
        newEntry.forget(null);
      if (newEntry.offload() != Boolean.TRUE)
        newEntry.offload(null);

      b.entries().put(newKey, newEntry.build());
    }
    b.version(ArchiveRegistry.DEFAULT_INSTANCE.version());
    return b.build();
  }

  /**
   * Updating the hidden registry is different, since we are copying many things
   * from the global registry
   */
  private ArchiveRegistry updateHiddenRegistry(ArchiveRegistry hiddenRegistry,
      ArchiveRegistry globalRegistry) {
    ArchiveRegistry.Builder updatedRegistry = ArchiveRegistry.newBuilder();

    for (Entry<String, ArchiveEntry> ent : globalRegistry.entries().entrySet()) {
      String key = ent.getKey();
      ArchiveEntry globalEntry = ent.getValue();

      ArchiveEntry originalHiddenEntry = hiddenRegistry.entries().get(key);
      if (originalHiddenEntry == null) {
        log("...no hidden entry found for:", key);
        originalHiddenEntry = ArchiveEntry.DEFAULT_INSTANCE;
      }

      int newVersion = originalHiddenEntry.version();
      if (newVersion > globalEntry.version()) {
        setError("*** entry", key, "hidden entry version", newVersion, "exceeds global",
            globalEntry.version());
      }

      ArchiveEntry.Builder updatedEnt = ArchiveEntry.newBuilder();
      updatedEnt.offload(originalHiddenEntry.offload());
      updatedEnt.version(originalHiddenEntry.version());

      updatedRegistry.entries().put(key, updatedEnt.build());
    }
    return updatedRegistry.build();
  }

  private void flushRegistries() {
    if (!mRegistryGlobalOriginal.equals(mRegistryGlobal)) {
      log("...global registry has been modified, writing updated version");
      files().writePretty(registerGlobalFile(), mRegistryGlobal);
    }

    if (!mRegistryLocalOriginal.equals(mRegistryLocal)) {
      log("...hidden registry has been modified, writing updated version");
      files().writePretty(registerLocalFile(), mRegistryLocal);
    }
  }

  private ArchiveEntry hiddenEntry() {
    return mRegistryLocal.entries().getOrDefault(mKey, ArchiveEntry.DEFAULT_INSTANCE);
  }

  private void storeHidden(ArchiveEntry entry) {
    mRegistryLocal.entries().put(mKey, entry.build());
  }

  private void storeLocalVersion(int version) {
    ArchiveEntry ent = hiddenEntry();
    if (ent.version() == version)
      return;
    storeHidden(ent.toBuilder().version(version));
  }

  private void updateEntries() {
    Map<String, ArchiveEntry> modifiedEntries = hashMap();

    for (Entry<String, ArchiveEntry> ent : mRegistryGlobal.entries().entrySet()) {
      ArchiveEntry entry = ent.getValue();

      mKey = ent.getKey();
      mEntry = entry.toBuilder();
      ArchiveEntry origHidden = hiddenEntry();
      mHiddenEntry = origHidden.toBuilder();
      mSourceFile = absoluteFileForEntry(mKey, mEntry);

      updateEntry();

      ArchiveEntry updatedEntry = mEntry.build();
      if (!updatedEntry.equals(entry)) {
        log("...storing new version of entry:", mKey, INDENT, updatedEntry);
        modifiedEntries.put(mKey, updatedEntry);
      }
      ArchiveEntry updatedHidden = mHiddenEntry.build();
      if (!updatedHidden.equals(origHidden))
        mRegistryLocal.entries().put(mKey, updatedHidden);
    }
    mRegistryGlobal.entries().putAll(modifiedEntries);
  }

  private void processForgetFlags() {
    List<String> keysToDelete = arrayList();

    for (Entry<String, ArchiveEntry> ent : mRegistryGlobal.entries().entrySet()) {
      ArchiveEntry entry = ent.getValue();
      mKey = ent.getKey();
      if (entry.forget() == Boolean.TRUE)
        keysToDelete.add(mKey);
    }

    mForgottenCount = keysToDelete.size();
    if (!keysToDelete.isEmpty()) {
      log("...forgetting:", keysToDelete);
      mRegistryGlobal.entries().keySet().removeAll(keysToDelete);
      mRegistryLocal.entries().keySet().removeAll(keysToDelete);
    }
  }

  private File fileWithinProjectDir(File relativePath) {
    return fileWithinProjectDir(relativePath.toString());
  }

  private File fileWithinProjectDir(String relativeFilePath) {
    return new File(mProjectDirectory, Files.assertRelative(relativeFilePath));
  }

  /**
   * Given a path, convert it to a file relative to the project directory. Fail
   * if it is not within the project directory
   */
  private File relativeToProjectDirectory(File file) {
    File origFile = file;
    file = Files.getCanonicalFile(file);
    String fPath = file.toString();
    String pPath = mProjectDirectory.toString();
    if (!fPath.startsWith(pPath))
      setError("file is not within project directory:", file, INDENT, "original argument:", origFile);
    return new File(fPath.substring(pPath.length() + 1));
  }

  private File relativeToProjectDirectory(String pathArg) {
    return relativeToProjectDirectory(new File(pathArg));
  }

  private void markForPushing(String userArg) {
    String key = optKeyFromUserArg(userArg);

    if (key == null) {

      // User wants to push an object that is not in the archive

      File file = new File(userArg);
      if (!file.exists())
        setError("No such file:", file);

      File relativeToProject = relativeToProjectDirectory(userArg);
      key = relativeToProject.getName();
      // If an entry already exists for this key, that is a problem
      ArchiveEntry existingEntry = mRegistryGlobal.entries().get(key);
      if (existingEntry != null)
        setError("entry already exists for key", key, "derived from path", userArg, ":", INDENT,
            existingEntry);

      ArchiveEntry.Builder b = ArchiveEntry.newBuilder();
      b.path(relativeToProject);
      if (file.isDirectory())
        b.directory(true);
      log("...creating new entry with id", key);
      mRegistryGlobal.entries().put(key, b.build());
    }

    ArchiveEntry entry = entryForKey(key);

    todo("Fail if user has marked this as forgotten, or offloaded");
    ArchiveEntry updatedEntry = entry.toBuilder().push(true).build();
    if (!updatedEntry.equals(entry)) {
      log("...marking for push:", key);
      mRegistryGlobal.entries().put(key, updatedEntry);
    } else
      pr("...already marked for push:", key);
  }

  private String optKeyForFile(File file) {
    List<String> foundKeys = arrayList();

    for (Entry<String, ArchiveEntry> ent : mRegistryGlobal.entries().entrySet()) {
      String key = ent.getKey();
      ArchiveEntry entry = ent.getValue();
      if (entry.path().equals(file))
        foundKeys.add(key);
    }
    if (foundKeys.isEmpty())
      return null;

    if (foundKeys.size() > 1)
      setError("Multiple keys found for path:", file, INDENT, JSList.with(foundKeys));

    return first(foundKeys);
  }

  /**
   * Determine key from user argument. Returns null if no key/object pair found
   */
  private String optKeyFromUserArg(String userArg) {
    String key = null;
    ArchiveEntry entry = mRegistryGlobal.entries().get(userArg);
    if (entry != null) {
      key = userArg;
    } else {
      // Assume the user argument is a path.  If it is relative, we treat it as if it is relative to the
      // current directory (as opposed to the project root directory).
      File path = relativeToProjectDirectory(userArg);
      key = optKeyForFile(path);
    }
    return key;
  }

  private ArchiveEntry entryForKey(String key, Object... errorMessageIfNotFound) {
    ArchiveEntry entry = mRegistryGlobal.entries().get(key);
    if (entry == null && errorMessageIfNotFound.length > 0)
      setError(errorMessageIfNotFound);
    return entry;
  }

  private void markForForgetting(String userArg) {
    String key = optKeyFromUserArg(userArg);
    ArchiveEntry foundEntry = entryForKey(key, "No object found for:", userArg);
    if (foundEntry.push() == Boolean.TRUE || foundEntry.offload() == Boolean.TRUE)
      setError("Object has unexpected state:", key, INDENT, foundEntry);

    ArchiveEntry updatedEntry = foundEntry.toBuilder().forget(true).build();
    if (!updatedEntry.equals(foundEntry)) {
      log("Marking for forget:", key);
      mRegistryGlobal.entries().put(key, updatedEntry);
    } else
      pr("Already marked for forget:", key);
  }

  private void markForOffloading(String userArg) {
    String key = optKeyFromUserArg(userArg);
    ArchiveEntry foundEntry = entryForKey(key, "No object found for:", userArg);
    if (foundEntry.version() == 0 || foundEntry.push() == Boolean.TRUE || foundEntry.forget() == Boolean.TRUE)
      setError("Object has unexpected state:", key, INDENT, foundEntry);
    ArchiveEntry updatedEntry = foundEntry.toBuilder().offload(true).build();
    if (!updatedEntry.equals(foundEntry)) {
      log("Marking for offloading:", key);
      mRegistryGlobal.entries().put(key, updatedEntry);
    } else
      pr("Already marked for offload:", key);
  }

  private void validateEntryStates(ArchiveEntry entry, ArchiveEntry hiddenEntry, File sourceFile) {
    todo("finish this");
  }

  private void updateEntry() {

    validateEntryStates(mEntry, mHiddenEntry, mSourceFile);

    if (mHiddenEntry.offload() == Boolean.TRUE) {
      log("Ignoring offloaded entry:", mKey);
      return;
    }

    // If item has never been pushed, do so
    if (mEntry.version() == 0 && mEntry.push() != Boolean.TRUE) {
      log("Entry has never been pushed, doing so:", mKey);
      Files.assertExists(mSourceFile);
      if (mSourceFile.isDirectory()) {
        mEntry.directory(true);
      }
      mEntry.push(true);
    }

    // Push new version from local to cloud if push signal was given
    //
    todo("the offload flag should be consulted before this illegal state is allowed to occur");
    if (mEntry.push() == Boolean.TRUE) {
      if (mHiddenEntry.offload() == Boolean.TRUE)
        throw badState("attempt to push offloaded entry:", mEntry);
      pushEntry();
      mPushedCount++;
      return;
    }

    // If user has requested to offload this entry, flag this fact within the hidden registry,
    // and delete the local copy
    //
    if (mEntry.offload() == Boolean.TRUE) {
      log("...offloading entry:", mKey);
      mOffloadedCount++;
      mEntry.offload(null);
      mHiddenEntry.offload(true);
      File sourcePath = mSourceFile;
      if (sourcePath.exists()) {
        log("...deleting local copy of offloaded entry:", mKey);
        if (sourcePath.isDirectory())
          files().deleteDirectory(sourcePath);
        else
          files().deleteFile(sourcePath);
      }
      return;
    }
    //    
    //    if (!mSourceFile.exists()) {
    //      mRegistryLocal.entries().remove(mKey);
    //    }
    int mostRecentVersion = Math.max(1, mEntry.version());
    if (mostRecentVersion == hiddenEntry().version())
      return;
    pullVersion(mostRecentVersion);
    mPulledCount++;
  }

  private boolean singleFile() {
    return mEntry.directory() != Boolean.TRUE;
  }

  /**
   * Determine name of file within archive corresponding to a version of an
   * object
   */
  private String filenameWithVersion(int version) {
    String basename = Files.basename(mKey);
    String ext;
    if (singleFile())
      ext = Files.getExtension(mEntry.path());
    else
      ext = "zip";
    if (ext.isEmpty())
      return String.format("%s_%03d", basename, version);
    else
      return String.format("%s_%03d.%s", basename, version, ext);
  }

  private void pushEntry() {

    // If path is empty, derive one from the key
    if (Files.empty(mEntry.path()))
      mEntry.path(new File(mKey));

    int nextVersionNumber = mEntry.version() + 1;
    String versionedFilename = filenameWithVersion(nextVersionNumber);
    log("...pushing version " + nextVersionNumber, "of:", mKey, "to", versionedFilename);
    log("...source:", mSourceFile);

    if (device().fileExists(versionedFilename))
      setError("Version", versionedFilename, "already exists in cloud");

    File sourceFile;
    if (singleFile()) {
      if (specificFilesOnly())
        setError("file_ext can only be specified for directories;", mKey);
      sourceFile = mSourceFile;
    } else
      sourceFile = createZipFile(mSourceFile);

    if (!files().dryRun()) {
      device().push(sourceFile, versionedFilename);
    }

    if (!singleFile())
      files().deleteFile(sourceFile);

    mEntry.version(nextVersionNumber);
    mEntry.push(null);
    storeLocalVersion(mEntry.version());
  }

  private void pullVersion(int desiredVersion) {
    log("...pulling version " + desiredVersion, "of:", mKey);
    String versionedFilename = filenameWithVersion(desiredVersion);

    files().deleteFile(tempFile());

    if (!files().dryRun()) {
      device().pull(versionedFilename, tempFile());
    }

    if (singleFile()) {
      if (mSourceFile.exists())
        createBackupOfOldLocalVersion(mKey, mSourceFile, false);

      files().mkdirs(mSourceFile.getParentFile());
      files().moveFile(tempFile(), mSourceFile);
    } else {
      if (specificFilesOnly()) {
        if (mSourceFile.exists()) {
          createBackupOfOldLocalVersion(mKey, mSourceFile, false);
          // Delete old versions of the types of extensions we want to restore
          for (File relFile : filesToZip(mSourceFile)) {
            files().deleteFile(new File(mSourceFile, relFile.toString()));
          }
        } else {
          files().mkdirs(mSourceFile);
        }

        if (!files().dryRun()) {
          Predicate<File> filter = null;
          if (specificFilesOnly())
            filter = (f) -> {
              String ext = Files.getExtension(f);
              if (mEntry.fileExtensions().contains(ext))
                return true;
              pr("*** Skipping file with unexpected extension, key:", mKey, INDENT, f);
              return false;
            };
          Files.unzip(tempFile(), mSourceFile, filter);
        }
      } else {
        File target = fileWithinProjectDir("_SKIP_unzip_temp");
        files().deleteDirectory(target);
        files().mkdirs(target);
        if (!files().dryRun())
          Files.unzip(tempFile(), target, null);

        if (mSourceFile.exists())
          createBackupOfOldLocalVersion(mKey, mSourceFile, true);

        files().mkdirs(mSourceFile.getParentFile());
        files().moveDirectory(target, mSourceFile);
      }
    }

    files().deleteFile(tempFile());

    storeLocalVersion(desiredVersion);
  }

  private void createBackupOfOldLocalVersion(String backupName, File sourceFileOrDirectory,
      boolean deleteOriginalDirectory) {
    File backupsDir = files().mkdirs(Files.getDesktopFile("_archive_backup_/" + backupName));
    List<File> backups = arrayList();
    for (File f : Files.files(backupsDir)) {
      if (RegExp.patternMatchesString("\\d+", f.getName()))
        backups.add(f);
    }

    int version = 0;
    if (!backups.isEmpty())
      version = 1 + Integer.parseInt(last(backups).getName());

    File target = new File(backupsDir, String.format("%05d", version));
    log("...saving backup:", target);

    if (sourceFileOrDirectory.isDirectory()) {
      if (deleteOriginalDirectory) {
        files().moveDirectory(sourceFileOrDirectory, target);
      } else {
        files().copyDirectory(sourceFileOrDirectory, target);
      }
    } else
      files().moveFile(sourceFileOrDirectory, target);

    while (backups.size() >= 3) {
      File oldBackupFileOrDirectory = backups.remove(0);
      log("...deleting old backup:", oldBackupFileOrDirectory);
      if (oldBackupFileOrDirectory.isDirectory())
        files().deleteDirectory(oldBackupFileOrDirectory);
      else
        files().deleteFile(oldBackupFileOrDirectory);
    }
  }

  private File tempFile() {
    return mTemporaryFile;
  }

  private boolean specificFilesOnly() {
    return mEntry.fileExtensions() != null;
  }

  private List<File> filesToZip(File directory) {
    DirWalk dirWalk = new DirWalk(directory);
    if (specificFilesOnly()) {
      dirWalk.withExtensions(mEntry.fileExtensions());
      dirWalk.withRecurse(false);
      todo("there is probably no need to have this 'no recurse' limitation");
    }

    List<File> result = arrayList();
    for (File relFile : dirWalk.filesRelative()) {
      String name = relFile.getName();
      if (name.startsWith("_SKIP_") || name.startsWith("_OLD_") || name.equals(".DS_Store"))
        continue;
      result.add(relFile);
    }
    return result;
  }

  private File createZipFile(File directory) {
    files().deleteFile(tempFile());

    try {
      ZipOutputStream zipStream = new ZipOutputStream(files().outputStream(tempFile()));
      for (File relFile : filesToZip(directory)) {
        String relPath = relFile.toString();
        ZipEntry zipEntry = new ZipEntry(relPath);
        zipStream.putNextEntry(zipEntry);
        zipStream.write(Files.toByteArray(new File(directory, relPath)));
        zipStream.closeEntry();
      }

      zipStream.close();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }

    checkState(tempFile().exists(), "failed to create: " + tempFile());
    return tempFile();
  }

  // ------------------------------------------------------------------
  // ArchiveEntry utilities
  // ------------------------------------------------------------------

  /**
   * Determine the absolute file corresponding to the local copy of an object
   */
  private File absoluteFileForEntry(String key, ArchiveEntry entry) {
    String pathString = key;
    if (!Files.empty(entry.path()))
      pathString = entry.path().toString();
    return fileWithinProjectDir(pathString);
  }

  private ArchiveDevice device() {
    if (mDevice == null) {
      if (Files.nonEmpty(mMockRemoteDir))
        mDevice = new FileArchiveDevice(mMockRemoteDir);
      else {
        File authFile = files().fileWithinSecrets("s3_auth.json");
        JSMap m = JSMap.from(authFile);
        mDevice = new S3Archive(m.get("profile"), m.get("account_name") + "/archive", mProjectDirectory);
      }
      mDevice.setDryRun(dryRun());
    }
    return mDevice;
  }

  // ------------------------------------------------------------------

  private boolean mPerformFlag;
  private boolean mValidateOnly;
  private File mProjectDirectory;
  private File mTemporaryFile;

  private ArchiveRegistry mRegistryGlobalOriginal;
  private ArchiveRegistry.Builder mRegistryGlobal;

  private ArchiveRegistry mRegistryLocalOriginal;
  private ArchiveRegistry.Builder mRegistryLocal;

  private int mPushedCount;
  private int mPulledCount;
  private int mOffloadedCount;
  private int mForgottenCount;

  private String mKey;
  private ArchiveEntry.Builder mEntry;
  private ArchiveEntry.Builder mHiddenEntry;
  private File mSourceFile;
  private String mPushPathArg;
  private String mForgetPathArg;
  private String mOffloadPathArg;
  private File mMockRemoteDir;
  private ArchiveDevice mDevice;
}
