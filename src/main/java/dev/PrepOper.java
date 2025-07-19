package dev;

import static js.base.Tools.*;

import dev.gen.PrepConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.DFA;
import js.parsing.Scanner;

import java.io.File;
import java.util.*;

public class PrepOper extends AppOper {

  private static final String FILTER_FILENAME = ".filter";
  private static final String PROJECT_INFO_FILE = ".prep_project";

  private static final int MAX_BACKUP_SETS = 5;
  private static final boolean SINGLE_SET = false && alert("SINGLE_SET in effect");

  @Override
  public String userCommand() {
    return "prep";
  }

  @Override
  public String shortHelp() {
    return "prepare repository for commit";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem(" save | restore", "Operation (save: prepare for commit; restore: restore afterward)");
    hf.addItem("[ pattern_file <hhhhhhh> ]", "file describing deletion patterns");
    b.pr(hf);
    b.br();
    b.pr("Prepares source files for commit by deleting particular content");
  }

  @Override
  public PrepConfig defaultArgs() {
    return PrepConfig.DEFAULT_INSTANCE;
  }

  @Override
  public PrepConfig config() {
    if (mConfig == null) {
      mConfig = super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    files().withDryRun(dryRun());
    log("Project directory:", projectDir());
    log("Cache directory:", cacheDir());
    log("Saving:", saving(), "Restoring:", restoring());
    if (saving()) {
      doSave();
    } else {
      doRestore();
    }
  }

  private boolean saving() {
    if (mSaving == null) {
      if (config().save())
        mSaving = true;
      else if (config().restore())
        mSaving = false;
      else
        throw setError("Specify save or restore operation");
    }
    return mSaving;
  }

  private boolean restoring() {
    return !saving();
  }

  /**
   * Determine the project directory
   */
  private File projectDir() {
    if (mProjectDir == null) {
      var c = config().projectRoot();
      if (Files.empty(c))
        c = Files.currentDirectory();
      Files.assertDirectoryExists(c, "project_root");

      // Make sure project info file exists
      var inf = Files.join(c, PROJECT_INFO_FILE);
      Files.assertExists(inf, "PROJECT_INFO_FILE");
      mProjectInfoFileContent = Files.readString(inf);
      if (mProjectInfoFileContent.trim().isEmpty()) {
        log("project file is empty, using default");
        mProjectInfoFileContent =
            Files.readString(this.getClass(), "prep_default.txt");
      }
      mProjectDir = c;
    }
    return mProjectDir;
  }

  private String mProjectInfoFileContent;

  private String projectInfoFileContent() {
    if (mProjectInfoFileContent == null) {
      projectDir();
    }
    return mProjectInfoFileContent;
  }

  /**
   * Determine the project cache directory.  This is where it will store
   * sets of backups (each within a separate numbered subdirectory)
   */
  private File cacheDir() {
    if (mCacheDir == null) {
      var pathExp = config().cachePathExpr();
      if (nullOrEmpty(pathExp))
        pathExp = projectDir().toString();
      pathExp = pathExp.replaceAll("[\\x3a\\x5c\\x2f\\x20]", "_");
      log("cache dir:", projectDir(), "=>", pathExp);
      String rel = config().cacheFilename() + "/" + pathExp;
      if (Files.empty(config().cacheDir()))
        mCacheDir = Files.getDesktopFile(rel);
      else
        mCacheDir = new File(config().cacheDir(), rel);
      files().mkdirs(mCacheDir);
    }
    return mCacheDir;
  }


  private static class DirStackEntry {
    File directory;
    FilterState filterState;

    public DirStackEntry(FilterState state, File directory) {
      this.directory = directory;
      this.filterState = state;
    }

    public DirStackEntry withState(FilterState newFilterState) {
      return new DirStackEntry(newFilterState, directory);
    }

    public DirStackEntry withDirectory(File dir) {
      Files.assertDirectoryExists(dir, "withDirectory");
      return new DirStackEntry(filterState, dir);
    }
  }

  private FilterState processFilterFile(String content, FilterState currentState) {
    var newState = currentState.dup();
    for (var line : parseLinesFromTextFile(content)) {
      newState.addDeleteFile(line);
    }
    return newState;
  }

  private void doSave() {
    boolean changesMade = false;
    var initialState = prepareState();
    List<DirStackEntry> dirStack = arrayList();
    dirStack.add(new DirStackEntry(initialState, projectDir()));
    while (!dirStack.isEmpty()) {
      var entry = pop(dirStack);
      var state = entry.filterState;

      var filterFile = new File(entry.directory, FILTER_FILENAME);
      if (filterFile.exists()) {
        var content = files().readString(filterFile);
        state = processFilterFile(content, state);
        entry = entry.withState(state);
      }

      // Examine the files (or subdirectories) within this directory (without recursing)

      var walk = new DirWalk(entry.directory).withRecurse(false).includeDirectories().omitNames(".DS_Store");
      for (var sourceFileOrDir : walk.files()) {
        var name = sourceFileOrDir.getName();
        if (false && alert("omitting big.rs") && name.equals("big.rs")) {
          continue;
        }
        if (state.deleteFilenames().contains(name)) {
          log("...filtering entire file or dir:", name);
          saveFileOrDir(sourceFileOrDir);
          if (sourceFileOrDir.isDirectory())
            files().deleteDirectory(sourceFileOrDir, "generated");
          else
            files().deleteFile(sourceFileOrDir);
          changesMade = true;
          continue;
        }

        if (!sourceFileOrDir.isDirectory()) {
          var sourceFile = sourceFileOrDir;
          var ext = Files.getExtension(sourceFile);
          if (false && alert("skipping all but a.java") && !sourceFile.getName().equals("a.java")) {
            continue;
          }

          var dfa = dfaForExtension(ext);
          if (dfa != null) {
            var rel = Files.relativeToContainingDirectory(sourceFile, projectDir());
            log("file:", rel);
            var currText = Files.readString(sourceFile);
            applyFilter(currText, dfa);

            if (mMatchesWithinFile != 0) {
              changesMade = true;
              //log("...match found:", INDENT, rel);
              saveFileOrDir(sourceFile);
              // Write new filtered form
              var filteredContent = mNewText.toString();
              log("...writing filtered version of:", rel);
              files().writeString(sourceFile, filteredContent);
            }
          }
        } else {
          push(dirStack, entry.withDirectory(sourceFileOrDir));
        }
      }
    }

    if (!changesMade) {
      setError("No filter matches found... did you mean to do a restore instead?");
    }
  }

  private void saveFileOrDir(File absSourceFileOrDir) {
    // determine relative path from project directory
    var relativePath = Files.relativeToContainingDirectory(absSourceFileOrDir, projectDir()).toString();

    var dest = new File(getSaveDir(), relativePath);
    log("...saving:", relativePath);
    files().mkdirs(Files.parent(dest));
    if (absSourceFileOrDir.isDirectory()) {
      files().copyDirectory(absSourceFileOrDir, dest);
    } else {
      files().copyFile(absSourceFileOrDir, dest);
    }
  }

  private File getSaveDir() {
    if (mSaveDir == null) {
      var found = auxGetCacheDirs();
      while (found.size() > MAX_BACKUP_SETS || (!found.isEmpty() && SINGLE_SET)) {
        var oldest = found.remove(0);
        files().deleteDirectory(oldest, "prep_oper_cache");
      }
      int i = 0;
      if (!found.isEmpty()) {
        i = 1 + Integer.parseInt(last(found).getName());
      }
      var x = new File(cacheDir(), String.format("%08d", i));
      log("getSaveDir, candidate:", INDENT, Files.infoMap(x));
      mSaveDir = x;
    }
    return mSaveDir;
  }

  private File mSaveDir;

  private void doRestore() {
    var restDir = getRestoreDir();
    if (Files.empty(restDir))
      throw setError("No cache directories found to restore from");
    var w = new DirWalk(restDir);
    int restoreCount = 0;
    for (var f : w.filesRelative()) {

      File source = w.abs(f);
      File dest = new File(projectDir(), f.getPath());
      if (false) {
        pr("...wanted to restore rel file:", f, CR, "source:", source, CR, "dest:", dest);
        continue;
      }
      log("restoring:", INDENT, source, CR, dest);
      files().mkdirs(Files.parent(dest));
      files().copyFile(source, dest, true);
      restoreCount++;
    }
    if (restoreCount == 0)
      pr("*** No files were restored!");
    log("# files restored:", restoreCount);
  }

  /**
   * Construct a sorted list of all cache subdirectories found for the project
   */
  private List<File> auxGetCacheDirs() {
    var w = new DirWalk(cacheDir()).withRecurse(false).includeDirectories();
    List<File> found = arrayList();
    for (var f : w.files()) {
      if (f.isDirectory()) {
        found.add(f);
      }
    }
    found.sort(Files.COMPARATOR);
    log("found cache dirs:", INDENT, found);
    return found;
  }

  private File getRestoreDir() {
    if (mRestoreDir == null) {
      var found = auxGetCacheDirs();
      mRestoreDir = Files.DEFAULT;
      if (!found.isEmpty()) mRestoreDir = last(found);
    }
    return mRestoreDir;
  }

  private File mRestoreDir;

  private static final char ERASE_CHAR = 0x7f;

  private void applyFilter(String currText, DFA dfa) {

    mNewText = new StringBuilder(currText);
    mMatchesWithinFile = 0;

    var s = new Scanner(dfa, currText, -1);
    s.setAcceptUnknownTokens();

    if (false && alert("setting verbosity"))
      s.setVerbose();

    int cursor = 0;
    while (s.hasNext()) {
      var tk = s.read();
      var len = tk.text().length();
      if (!tk.isUnknown()) {
        if (verbose())
          log("...found matching token:", INDENT, tk);
        mMatchesWithinFile++;
        for (int i = 0; i < len; i++)
          mNewText.setCharAt(i + cursor, ERASE_CHAR);
      }
      cursor += len;
    }
    if (mMatchesWithinFile != 0)
      pass2();
  }

  private void pass2() {
    var s = mNewText;
    // Ensure file ends with linefeed
    addLF(s);

    var out = new StringBuilder();

    var lineContainedChars = false;
    int bufferedSpaceCount = 0;
    var eraseFound = false;
    int i = 0;
    while (i < s.length()) {
      var c = s.charAt(i++);
      if (c == '\n') {
        // If the line had some non-whitespace characters
        if (lineContainedChars || !eraseFound) {
          out.append('\n');
        }
        lineContainedChars = false;
        bufferedSpaceCount = 0;
        eraseFound = false;
      } else if (c <= ' ') {
        bufferedSpaceCount++;
      } else if (c == ERASE_CHAR) {
        eraseFound = true;
      } else {
        lineContainedChars = true;
        for (int j = 0; j < bufferedSpaceCount; j++) {
          out.append(' ');
        }
        bufferedSpaceCount = 0;
        out.append(c);
      }
    }
    mNewText.setLength(0);
    mNewText.append(out);
  }

  private PrepConfig mConfig;
  private File mProjectDir;
  private File mCacheDir;
  private Boolean mSaving;

  private int mMatchesWithinFile;
  private StringBuilder mNewText;

  /**
   * Read pattern file contents, if it exists; otherwise, read the contents of the one in the app resources
   */
  private String readPatternFile(File patFile) {
    // If no pattern file was found, use the one stored in the app resources
    String text;
    if (Files.nonEmpty(patFile)) {
      Files.assertExists(patFile, "pattern_file");
      text = Files.readString(patFile);
    } else
      text = Files.readString(this.getClass(), "prep_default.txt");
    return text;
  }

  private Set<String> mActiveExtensions;

  /**
   * Set up the initial filter state.
   *
   * We also parse the project info file, constructing the .rxp files for each supported file extension.
   * Then we regenerate the .dfa files from those .rxp files (if they aren't cached already)
   */
  private FilterState prepareState() {
    var text = projectInfoFileContent();

    mActiveExtensions = hashSet();

    for (var x : split(text, '\n')) {

      var extensionPrefix = ">>>";
      if (x.startsWith(extensionPrefix)) {

        mActiveExtensions.clear();
        x = chompPrefix(x, extensionPrefix);

        var extList = split(x, ',');
        for (var ext : extList) {
          checkArgument(ext.matches("[a-zA-Z]+"), "illegal extension:", ext);
          mActiveExtensions.add(ext);
        }
        continue;
      }

      for (var ext : mActiveExtensions) {
        var buffer = extensionBuffer(ext);
        buffer.append(x);
        buffer.append('\n');
      }

    }

    // Construct DFAs from each extension
    {
      for (var ent : mRXPContentForFileExtensionMap.entrySet()) {
        todo("Have a utility method in the js.parsing to regenerate .dfa files from strings if necessary");

        String ext = ent.getKey();
        String rxp = ent.getValue().toString();

        // Get DFA for this regexp
        String randomHash = UUID.nameUUIDFromBytes(rxp.getBytes()).toString();
        var dfaFile = new File(cacheDir(), randomHash + ".dfa");

        if (!dfaFile.exists()) {
          log("Generating dfa from:", INDENT, rxp);

          // Call the dfa program to compile regexps to a dfa
          var tmpDir = files().createTempDir("PrepOper_build_dfa");
          var inputFile = new File(tmpDir, "x.rxp");
          files().writeString(inputFile, rxp);
          var sc = new SystemCall().withVerbose(verbose());
          sc.arg("dfa", "input", inputFile, "output", dfaFile);
          sc.assertSuccess();
        }
        var dfa = DFA.parse(Files.readString(dfaFile));
        mDFAForFileExtensionMap.put(ext, dfa);
      }
    }

    Set<String> deleteFilenames = hashSet();
    deleteFilenames.add(FILTER_FILENAME);
    return new FilterState(deleteFilenames);
  }

  private StringBuilder extensionBuffer(String ext) {
    var result = mRXPContentForFileExtensionMap.get(ext);
    if (result == null) {
      result = new StringBuilder();
      mRXPContentForFileExtensionMap.put(ext, result);
    }
    return result;
  }

  private Map<String, StringBuilder> mRXPContentForFileExtensionMap = hashMap();

  private static List<String> parseLinesFromTextFile(String text) {

    List<String> out = arrayList();
    for (var x : split(text, '\n')) {
      x = x.trim();
      if (x.startsWith("#")) continue;
      if (x.isEmpty()) continue;
      out.add(x);
    }
    return out;
  }

  private DFA dfaForExtension(String ext) {
    return mDFAForFileExtensionMap.get(ext);
  }

  private Map<String, DFA> mDFAForFileExtensionMap = hashMap();


  private static class FilterState {

    public FilterState(Collection<String> deleteFilenames) {
      mDeleteFilenames = hashSet();
      mDeleteFilenames.addAll(deleteFilenames);
    }

    private FilterState() {
    }

    public Set<String> deleteFilenames() {
      return mDeleteFilenames;
    }

    public FilterState dup() {
      var s = new FilterState();
      s.mDeleteFilenames = new HashSet<String>(mDeleteFilenames);
      return s;
    }

    public void addDeleteFile(String filename) {
      mDeleteFilenames.add(filename);
    }

    // This map should be considered immutable.  If changes are made, construct a new copy
    private Set<String> mDeleteFilenames;
  }
}
