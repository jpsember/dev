package dev;

import static js.base.Tools.*;

import dev.gen.AppInfo;
import dev.gen.PrepConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.RegExp;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class PrepOper extends AppOper {

  private static final String FILTER_FILENAME = ".filter";

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
      mProjectDir = Files.assertDirectoryExists(c,"project_root");
    }
    return mProjectDir;
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


  private static class PatternRecord {

    @Override
    public int hashCode() {
      return description.hashCode();
    }

    @Override
    public boolean equals(Object object) {
      if (this == object)
        return true;
      if (object == null || !(object instanceof AppInfo))
        return false;
      PatternRecord other = (PatternRecord) object;
      return other.description.equals(description);
    }

    PatternRecord(String description) {
      this.description = description;
      this.pattern = RegExp.pattern(description);
    }

    Pattern pattern;
    String description;

    @Override
    public String toString() {
      return description;
    }
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

      var w = new DirWalk(entry.directory).withRecurse(false).includeDirectories().omitNames(".DS_Store");
      for (var sourceFileOrDir : w.files()) {
        var name = sourceFileOrDir.getName();
        if (state.deleteFilenames().contains(name)) {
          log("...filtering entire file or dir:", name);
          saveFileOrDir(sourceFileOrDir, w.rel(sourceFileOrDir).toString());
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
          var patterns = state.patterns().optPatternsForExt(ext);
          if (!patterns.isEmpty()) {
            log("file:", w.rel(sourceFile));
            var currText = Files.readString(sourceFile);
            applyFilter(currText, patterns);

            if (mMatchesWithinFile != 0) {
              changesMade = true;
              var rel = w.rel(sourceFile);
              log("...match found:", INDENT, rel);
              saveFileOrDir(sourceFile, rel.toString());
              // Write new filtered form
              var filteredContent = mNewText.toString();
              log("...writing filtered version of:", rel, INDENT, filteredContent);
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

  private void saveFileOrDir(File absSourceFileOrDir, String relativePath) {
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

  private void applyFilter(String currText, Collection<PatternRecord> patterns) {

    mNewText = new StringBuilder(currText);
    mMatchesWithinFile = 0;

    // Apply each of the patterns
    for (var p : patterns) {
      var m = p.pattern.matcher(currText);
      while (m.find()) {
        // Make sure the text is either at the start of a line, or
        // is preceded by some whitespace.
        var start = m.start();
        var end = m.end();

        if (verbose())
          log("...found match for pattern:", p, "at start:", start, "to:", end, "text:", currText.substring(start, end));

        if (!(start == 0 || currText.charAt(start - 1) <= ' ')) {
          log("...pattern does NOT occur at start of line, ignoring");
          continue;
        }

        for (int i = start; i < end; i++)
          mNewText.setCharAt(i, ERASE_CHAR);

        mMatchesWithinFile++;
      }
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

  private File findPatternFile() {
    var patFile = config().patternFile();

    if (!config().skipPatternSearch()) {

      // If the configuration value was missing,
      // look for it in this order:
      //
      // a) the current directory
      // b) an ancestor directory up to and including the project directory
      // c) the home directory

      if (Files.empty(patFile)) {
        var dir = Files.currentDirectory();
        while (true) {
          var c = new File(dir, ".prep_patterns.txt");
          if (c.exists()) {
            patFile = c;
            break;
          }
          if (dir.equals(projectDir()))
            break;
          dir = Files.parent(dir);
        }
      }

      if (Files.empty(patFile)) {
        var c = new File(Files.homeDirectory(), ".prep_patterns.txt");
        if (c.exists())
          patFile = c;
      }
    }
    return patFile;
  }

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

  /**
   * Set up the initial filter state
   */
  private FilterState prepareState() {
    var patFile = findPatternFile();
    var text = readPatternFile(patFile);

    var patterns = new PatternCollection();

    // Parse the pattern file text

    Set<String> activeExtensions = hashSet();

    for (var x : split(text, '\n')) {
      x = x.trim();
      if (x.startsWith("#")) continue;
      if (x.isEmpty()) continue;

      if (x.startsWith("{omit}")) {
        pr("...found unsupported content:", x);
        continue;
      }
      var extensionPrefix = ">>>";
      if (x.startsWith(extensionPrefix)) {
        activeExtensions.clear();
        x = chompPrefix(x, extensionPrefix);

        var extList = split(x, ',');
        for (var ext : extList) {
          checkArgument(ext.matches("[a-zA-Z]+"), "illegal extension:", ext);
          activeExtensions.add(ext);
        }
        continue;
      }

      // If the pattern ends with '(;', replace this suffix with
      // something that accepts any amount of text following ( that does NOT include
      // a semicolon, followed by a semicolon.
      var y = chomp(x, "(;");
      if (y != x)
        x = y + "\\x28[^;]*;";

      var rec = new PatternRecord(x);
      for (var ext : activeExtensions) {
        patterns.add(ext, rec);
      }
    }

    Set<String> deleteFilenames = hashSet();
    deleteFilenames.add(FILTER_FILENAME);
    return new FilterState(patterns, deleteFilenames);
  }

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

  private static class PatternCollection {
    public PatternCollection() {
      mPatternBank = hashMap();
    }

    public PatternCollection dup() {
      var x = new PatternCollection();
      for (var ent : mPatternBank.entrySet()) {
        x.mPatternBank.put(ent.getKey(), new HashSet<>(ent.getValue()));
      }
      return x;
    }

    public void add(String extension, PatternRecord pattern) {
      var x = getPatternsForExt(extension);
      x.add(pattern);
    }

    private Set<PatternRecord> getPatternsForExt(String extension) {
      var x = mPatternBank.get(extension);
      if (x == null) {
        x = hashSet();
        mPatternBank.put(extension, x);
      }
      return x;
    }

    public Set<PatternRecord> optPatternsForExt(String extension) {
      var x = mPatternBank.get(extension);
      if (x == null)
        return DataUtil.emptySet();
      return x;
    }

    private Map<String, Set<PatternRecord>> mPatternBank;
  }

  private static class FilterState {

    public FilterState(PatternCollection patterns, Collection<String> deleteFilenames) {
      mPatterns = patterns;
      mDeleteFilenames = hashSet();
      mDeleteFilenames.addAll(deleteFilenames);
    }

    private FilterState() {
    }

    public PatternCollection patterns() {
      return mPatterns;
    }

    public Set<String> deleteFilenames() {
      return mDeleteFilenames;
    }

    public FilterState dup() {
      var s = new FilterState();
      s.mPatterns = mPatterns.dup();
      s.mDeleteFilenames = new HashSet<String>(mDeleteFilenames);
      return s;
    }

    public void addDeleteFile(String filename) {
      mDeleteFilenames.add(filename);
    }

    // This map should be considered immutable.  If changes are made, construct a new copy
    private PatternCollection mPatterns;
    private Set<String> mDeleteFilenames;

  }
}
