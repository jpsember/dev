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
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import js.file.DirWalk;
import js.file.Files;
import js.app.AppOper;
import js.base.BasePrinter;
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
 * Edit archive_registry.json, inserting a key/value pair to the "entries" map:
 * 
 *        "abc/xyz/foo" : { }
 * 
 * In the above example, the relative path is the key.  Alternatively, an arbitrary key, e.g. "moo", can be used:
 *  
 *        "moo" : { "path" : "abc/xyz/foo" }
 *        
 * Run the archive operation to push the file to the archive:
 * 
 *    dev archive
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
 *    dev archive push abc/xyz/foo
 *    dev archive
 *    
 * The first line registers the object with the local registry.  The second call actually performs the pushing
 * to the archive.  Multiple files can be added before a final "dev archive" call is made.
 *    
 * Alternatively, if the object has a key that is different from its path (e.g. "moo"):
 * 
 *    dev archive push moo
 *    dev archive
 *    
 * 
 * Removing an object from the archive
 * ------------------------------------------------------------------------------------
 * To make the archive stop tracking "<project root directory>/abc/xyz/foo":
 * 
 *    dev archive forget abc/xyz/foo
 *    dev archive
 *    
 * or, if appropriate,
 * 
 *    dev archive forget moo
 *    dev archive
 * 
 * Note that neither the local copy of an object, nor any previously pushed versions in the external data store,
 * are deleted by this operation.  It only makes the archive "forget" about the object.  It must be manually deleted
 * (if desired) from the local machine or the data store.
 * 
 * 
 * </pre>
 * 
 * 
 * TODO: when a directory is archived, e.g. aaa/bbb/ccc, it gets saved as a zip
 * file within the archive root directory, e.g. ccc.zip. This is a problem if
 * there are multiple directories named ccc, but with different paths, as they
 * will attempt to use the same filename(s) within the archive. It would
 * probably be better if the filename is based on the full (relative) path, e.g.
 * "aaa_bbb_ccc.zip" (though this has problems as '_' is a valid path character,
 * so aaa/bbb/ccc/ would conflict with aaa_bbb_ccc/).
 * 
 * Note also that underscores cannot appear in bucket names in S3.
 * 
 * TODO: extensions are ignored when determining the 'archived' name of an
 * object; so 'aaa/bbb/ccc.txt' and 'ddd/eee/ccc.json' would both be assigned
 * the 'archived' name (without version info) 'ccc.zip'.
 * 
 * TODO: a reasonable solution to these problems is to scan the archive when the
 * operation is run, looking for conflicts with the archive names, and failing
 * immediately if any are found. It should perhaps also require that directories
 * don't have extensions (e.g. foo.bar is illegal), and that non-directories
 * MUST have extensions. Also, that paths are relative.
 * 
 */
public final class ArchiveOper extends AppOper {

  private static final boolean DUMP_REG = false && alert("dumping registries");

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
        "[validate] : perform validation only (for test purposes)");
  }

  @Override
  protected void processAdditionalArgs() {
    todo("be careful to 'canonicalize' paths where appropriate");
    mProjectDirectory = new File(cmdLineArgs().nextArgIf("dir", ""));
    mMockRemoteDir = new File(cmdLineArgs().nextArgIf("mock_remote", ""));
    mPushPathArg = cmdLineArgs().nextArgIf("push", "");
    mForgetPathArg = cmdLineArgs().nextArgIf("forget", "");
    mValidateOnly = cmdLineArgs().nextArgIf("validate");
  }

  /**
   * If a file is nonempty, get its canonical version
   */
  private File fixPath(File file) {
    if (Files.nonEmpty(file))
      return file;
    return Files.getCanonicalFile(file);
  }

  private void fixPaths() {
    mProjectDirectory = fixPath(mProjectDirectory);
    mMockRemoteDir = fixPath(mMockRemoteDir);
    if (Files.empty(mProjectDirectory))
      mProjectDirectory = Files.getCanonicalFile(Files.parent(Files.S.projectConfigDirectory()));
    mTemporaryFile = fileWithinProjectDir("_SKIP_temp.zip");
  }

  @Override
  public void perform() {
    fixPaths();

    readRegistry();
    if (mValidateOnly)
      return;

    boolean proc = false;

    if (nonEmpty(mPushPathArg)) {
      proc = true;
      markForPushing(mPushPathArg);
    }
    if (nonEmpty(mForgetPathArg)) {
      proc = true;
      markForForgetting(mForgetPathArg);
    }

    if (proc) {
      flushRegistry();
    } else {
      processForgetFlags();
      updateEntries();
      flushRegistry();
      log(map().put("entries", mRegistryGlobal.entries().size())//
          .put("pushed", mPushedCount)//
          .put("pulled", mPulledCount)//
          .put("offloaded", mOffloadedCount)//
          .put("forgotten", mForgottenCount)//
      );
    }
  }

  private static final Pattern RELATIVE_PATH_PATTERN = RegExp
      .pattern("^\\.?\\w+(?:\\/\\.?\\w+)*(?:\\.\\w+)?$");

  private String contextExpr(Object contextOrNull) {
    return "(" + nullTo(contextOrNull, "no context given") + ")";
  }

  private void validateRegistry(ArchiveRegistry registry, String context) {
    BasePrinter p = new BasePrinter();

    String expected = ArchiveRegistry.DEFAULT_INSTANCE.version();
    if (!registry.version().equals(expected))
      p.pr("*** Bad version number:", registry.version(), "; expected", expected);
    else {
      for (Entry<String, ArchiveEntry> entry : registry.entries().entrySet()) {
        ArchiveEntry ent = entry.getValue();
        File pt = ent.path();
        if (Files.nonEmpty(pt)) {
          if (!RegExp.patternMatchesString(RELATIVE_PATH_PATTERN, pt.toString())) {
            p.pr("*** Illegal path for:", entry.getKey(), INDENT, ent);
          }
        }
      }
    }

    String errorMessage = p.toString();
    if (nonEmpty(errorMessage))
      setError("Problems with archive registry", contextExpr(context), ":", INDENT, errorMessage);
  }

  private void readRegistry() {
    todo("refactor to better determine if registry (normal and hidden) have changed");
    File globalFile = registerGlobalFile();
    ArchiveRegistry registry = Files.parseAbstractData(ArchiveRegistry.DEFAULT_INSTANCE, globalFile);
    if (DUMP_REG)
      pr("global registry:", INDENT, registry);
    validateRegistry(registry, globalFile.getName());
    mRegistryGlobalOriginal = registry;
    mRegistryGlobal = registry.toBuilder();
    readHiddenRegistry();
  }

  private void ensureVersionValid(ArchiveRegistry registry, File context) {
    String expected = ArchiveRegistry.DEFAULT_INSTANCE.version();
    if (!registry.version().equals(expected))
      throw die("bad version in archive registry:", registry.version(), "expected:", expected, ";", context);
  }

  private File registerGlobalFile() {
    return fileWithinProjectDir("archive_registry.json");
  }

  private File registerLocalFile() {
    return fileWithinProjectDir(".archive_registry.json");
  }

  private void readHiddenRegistry() {
    ArchiveRegistry registry = Files.parseAbstractDataOpt(ArchiveRegistry.DEFAULT_INSTANCE,
        registerLocalFile());
    ensureVersionValid(registry, registerLocalFile());

    mRegistryLocalOriginal = registry;

    if (DUMP_REG) {
      pr("local registry:", INDENT, registry);
    }

    // Apparently the toBuilder() call *does* construct a copy of the entries map, which is what we need
    // (since we want to leave the original registry untouched)
    mRegistryLocal = registry.toBuilder();
  }

  private void flushRegistry() {

    if (!mRegistryGlobalOriginal.equals(mRegistryGlobal)) {
      JSMap registryMap;
      {
        registryMap = mRegistryGlobal.toJson();

        // Make the registry more legible to a human by removing some unnecessary key/value pairs, where possible

        JSMap m2 = registryMap.getMap("entries");
        todo("make additional fields optional where appropriate");
        for (String k : m2.keySet()) {
          JSMap m = m2.getMap(k);
          if (!m.opt("push"))
            m.remove("push");
          if (m.get("path").isEmpty())
            m.remove("path");
          m.remove("offload");
          if (m.getList("file_extensions").isEmpty())
            m.remove("file_extensions");
        }
      }

      log("...global registry has been modified, writing updated version");
      files().writePretty(registerGlobalFile(), registryMap);
    }

    if (!mRegistryLocalOriginal.equals(mRegistryLocal)) {
      log("...hidden registry has been modified, writing updated version");
      files().writePretty(registerLocalFile(), mRegistryLocal);
    }
  }

  private ArchiveEntry hiddenEntry() {
    return mRegistryLocal.entries().getOrDefault(mKey, ArchiveEntry.DEFAULT_INSTANCE);
  }

  private void storeLocalVersion(int version) {
    ArchiveEntry ent = hiddenEntry();
    if (ent.version() == version)
      return;
    mRegistryLocal.entries().put(mKey, ent.toBuilder().version(version).build());
  }

  private void updateEntries() {

    Map<String, ArchiveEntry> modifiedEntries = hashMap();

    for (Entry<String, ArchiveEntry> ent : mRegistryGlobal.entries().entrySet()) {
      ArchiveEntry entry = ent.getValue();
      if (entry.ignore() == Boolean.TRUE)
        continue;

      todo("can we avoid instance fields mKey, mEntry?");
      mKey = ent.getKey();
      mEntry = entry.toBuilder();

      File file = absoluteFileForEntry(mKey, mEntry);

      if (Files.getExtension(file).isEmpty()) {
        mSourceFile = null;
        mSourceDirectory = file;
      } else {
        mSourceDirectory = null;
        mSourceFile = file;
      }

      updateEntry();

      ArchiveEntry updatedEntry = mEntry.build();
      if (updatedEntry.equals(entry))
        continue;

      log("...storing new version of entry:", mKey, INDENT, updatedEntry);
      modifiedEntries.put(mKey, updatedEntry);
    }
    mRegistryGlobal.entries().putAll(modifiedEntries);
    pr("after updating entries:", INDENT, mRegistryGlobal);
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
    pr("after forgetting:", keysToDelete, INDENT, mRegistryGlobal, CR, mRegistryLocal);
  }

  private File fileWithinProjectDir(String relativeFilePath) {
    checkArgument(relativeFilePath.charAt(0) != '/');
    return new File(mProjectDirectory, relativeFilePath);
  }

  /**
   * Given a path, convert it to a file relative to the project directory. Fail
   * if it is not within the project directory
   */
  private File relativeToProjectDirectory(String pathArg) {
    checkArgument(nonEmpty(pathArg));
    File f = new File(pathArg);
    if (!f.isAbsolute()) {
      f = new File(mProjectDirectory, pathArg);
    }
    f = Files.getCanonicalFile(f);

    String fPath = f.toString();
    String pPath = mProjectDirectory.toString();
    if (!fPath.startsWith(pPath))
      throw badArg("path is not within project directory:", pathArg);
    return new File(fPath.substring(pPath.length() + 1));
  }

  private void markForPushing(String pathArg) {
    File path = relativeToProjectDirectory(pathArg);
    String foundKey = findKeyForFileOrDir(path);
    ArchiveEntry foundEntry = mRegistryGlobal.entries().get(foundKey);
    ArchiveEntry updatedEntry = foundEntry.toBuilder().push(true).build();
    if (!updatedEntry.equals(foundEntry)) {
      pr("...marking for push:", foundKey);
      mRegistryGlobal.entries().put(foundKey, updatedEntry);
    } else
      pr("...already marked for push:", foundKey);
  }

  private String findKeyForFileOrDir(File fileOrDir) {

    String foundKey = null;

    // Look for entry matching this directory.
    // If we find an exact match, then prioritize that; otherwise, look for just a filename match.
    // But disallow multiple filename matches.

    List<File> filenameMatches = arrayList();
    List<String> filenameKeys = arrayList();
    String seekName = fileOrDir.getName();
    boolean performFuzzyMatch = !(fileOrDir.toString().contains("/"));

    for (Entry<String, ArchiveEntry> ent : mRegistryGlobal.entries().entrySet()) {
      String key = ent.getKey();
      ArchiveEntry entry = ent.getValue();
      File file = absoluteFileForEntry(key, entry);
      if (file.equals(fileOrDir)) {
        foundKey = key;
        break;
      }
      if (performFuzzyMatch) {
        if (file.getName().equals(seekName)) {
          filenameKeys.add(key);
          filenameMatches.add(file);
        }
      }
    }

    if (foundKey == null) {
      if (filenameMatches.size() == 1) {
        foundKey = filenameKeys.get(0);
      } else if (filenameKeys.size() > 1) {
        die("Multiple files found with name", seekName, ":", INDENT, filenameMatches);
      }
    }

    if (foundKey == null)
      die("No entry found for file:", fileOrDir);
    return foundKey;
  }

  private void markForForgetting(String pathArg) {
    File path = relativeToProjectDirectory(pathArg);
    String foundKey = findKeyForFileOrDir(path);
    ArchiveEntry foundEntry = mRegistryGlobal.entries().get(foundKey);
    ArchiveEntry updatedEntry = foundEntry.toBuilder().forget(true).build();
    if (!updatedEntry.equals(foundEntry)) {
      pr("...marking for forget:", foundKey);
      mRegistryGlobal.entries().put(foundKey, updatedEntry);
    } else
      pr("...already marked for forget:", foundKey);
  }

  private File sourceFileOrDirectory() {
    return singleFile() ? mSourceFile : mSourceDirectory;
  }

  private void updateEntry() {

    // Push new version from local to cloud if push signal was given
    //
    if (mEntry.version() == 0 || mEntry.push()) {
      pushEntry();
      mPushedCount++;
      return;
    }

    // If user has requested to offload this entry, flag this fact within the hidden registry,
    // and delete the local copy
    //
    if (mEntry.offload()) {
      log("...offloading entry:", mKey);
      mOffloadedCount++;
      mEntry.offload(false);
      mRegistryLocal.entries().put(mKey, hiddenEntry().toBuilder().offload(true).build());
    }

    if (hiddenEntry().offload()) {
      File sourcePath = sourceFileOrDirectory();
      if (sourcePath.exists()) {
        log("...deleting local copy of offloaded entry:", mKey);
        if (sourcePath.isDirectory())
          files().deleteDirectory(sourcePath);
        else
          files().deleteFile(sourcePath);
      }
      return;
    }

    if (!sourceFileOrDirectory().exists()) {
      mRegistryLocal.entries().remove(mKey);
    }
    int mostRecentVersion = Math.max(1, mEntry.version());

    if (mostRecentVersion == hiddenEntry().version())
      return;
    die("attempting to pull most recent version:", mostRecentVersion, "of", mKey, "since doesn't match:",
        INDENT, hiddenEntry());
    pullVersion(mostRecentVersion);
    mPulledCount++;
  }

  private boolean singleFile() {
    return mSourceFile != null;
  }

  /**
   * Determine name of file within archive corresponding to a version of an
   * object
   */
  private String filenameWithVersion(int version) {
    String basename = Files.basename(mKey);
    if (singleFile()) {
      String ext = Files.getExtension(mKey);
      // Note: this shouldn't happen if we run our 'sanity check' for keys when the operation starts
      checkArgument(!ext.isEmpty(), "filename has no extension:", mKey, INDENT, mEntry);
      return String.format("%s_%03d.%s", basename, version, ext);
    } else
      return String.format("%s_%03d.zip", basename, version);
  }

  private void pushEntry() {
    int nextVersionNumber = mEntry.version() + 1;
    String versionedFilename = filenameWithVersion(nextVersionNumber);
    log("...pushing version " + nextVersionNumber, "of:", mKey, "to", versionedFilename);
    log("...source:", sourceFileOrDirectory());

    if (device().fileExists(versionedFilename))
      throw die("Version", versionedFilename, "already exists in cloud");

    File sourceFile;
    if (singleFile()) {
      if (specificFilesOnly())
        throw die("file_ext can only be specified for directories; " + mKey);
      sourceFile = mSourceFile;
    } else
      sourceFile = createZipFile(mSourceDirectory);

    if (!files().dryRun()) {
      device().push(sourceFile, versionedFilename);
    }

    if (!singleFile())
      files().deleteFile(sourceFile);

    mEntry.version(nextVersionNumber);
    mEntry.push(false);
    storeLocalVersion(mEntry.version());
  }

  private void pullVersion(int desiredVersion) {
    log("...pulling version " + desiredVersion, "of:", mKey);
    String versionedFilename = filenameWithVersion(desiredVersion);

    Files.S.deleteFile(tempFile());

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
        if (mSourceDirectory.exists()) {
          createBackupOfOldLocalVersion(mKey, mSourceDirectory, false);
          // Delete old versions of the types of extensions we want to restore
          for (File relFile : filesToZip(mSourceDirectory)) {
            files().deleteFile(new File(mSourceDirectory, relFile.toString()));
          }
        } else {
          files().mkdirs(mSourceDirectory);
        }

        if (!files().dryRun()) {
          Files.unzip(tempFile(), mSourceDirectory, (f) -> {
            String ext = Files.getExtension(f);
            if (mEntry.fileExtensions().contains(ext))
              return true;
            pr("*** Skipping file with unexpected extension, key:", mKey, INDENT, f);
            return false;
          });
        }
      } else {
        File target = fileWithinProjectDir("_SKIP_unzip_temp");
        files().deleteDirectory(target);
        files().mkdirs(target);
        if (!files().dryRun())
          Files.unzip(tempFile(), target, null);

        if (mSourceDirectory.exists())
          createBackupOfOldLocalVersion(mKey, mSourceDirectory, true);

        files().mkdirs(mSourceDirectory.getParentFile());
        files().moveDirectory(target, mSourceDirectory);
      }
    }

    Files.S.deleteFile(tempFile());

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
    return !mEntry.fileExtensions().isEmpty();
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
    Files.S.deleteFile(tempFile());

    try {
      ZipOutputStream zipStream = new ZipOutputStream(Files.S.outputStream(tempFile()));
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
        die("S3Archive disabled for now");
        mDevice = new S3Archive("<profile name>", "<s3 account name>/archive", mProjectDirectory);
      }
    }
    return mDevice;
  }

  // ------------------------------------------------------------------

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
  private File mSourceFile;
  private File mSourceDirectory;
  private String mPushPathArg;
  private String mForgetPathArg;
  private File mMockRemoteDir;
  private ArchiveDevice mDevice;
}
