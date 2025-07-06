package dev;

import static js.base.Tools.*;

import dev.gen.PrepConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.RegExp;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class PrepOper extends AppOper {

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
    //alertVerbose();
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
   * Determine the project directory by looking in the config directory
   * (or the current directory, if none was specified) or one of its parents
   * for a ".git" subdirectory (or whatever the config.project_file specifies)
   */
  private File projectDir() {
    if (mProjectDir == null) {
      var begin = config().dir();
      if (Files.empty(begin)) {
        begin = Files.currentDirectory();
      }
      begin = Files.absolute(begin);
      // Find project root
      var cursor = begin;
      while (true) {
        var projectFile = new File(cursor, config().projectFile().toString());
        if (projectFile.exists()) {
          mProjectDir = cursor;
          break;
        }
        cursor = cursor.getParentFile();
        if (cursor == null)
          setError("Cannot find .git directory in parents of:", begin);
      }
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


//  private Map<String, List<PatternRecord>> getPatternBank() {
//    if (mPatternBank == null) {
//
//      mPatternBank = hashMap();
//
//      String text;
//      var patFile = config().patternFile();
//
//      if (!config().skipPatternSearch()) {
//        if (Files.empty(patFile)) {
//          var c = new File(".prep_patterns.txt");
//          if (c.exists()) patFile = c;
//        }
//        if (Files.empty(patFile)) {
//          var c = new File(Files.homeDirectory(), ".prep_patterns.txt");
//          if (c.exists())
//            patFile = c;
//        }
//      }
//
//      if (Files.nonEmpty(patFile)) {
//        Files.assertExists(patFile, "pattern_file");
//        text = Files.readString(patFile);
//      } else
//        text = Files.readString(this.getClass(), "prep_default.txt");
//
//      Set<String> activeExtensions = hashSet();
//
//      for (var x : parseLinesFromTextFile(text)) {
//
//        log(VERT_SP, "parsing pattern line:", INDENT, x);
//
//        var extensionPrefix = ">>>";
//        if (x.startsWith(extensionPrefix)) {
//          activeExtensions.clear();
//          x = chompPrefix(x, extensionPrefix);
//
//
//          var extList = split(x, ',');
//          for (var ext : extList) {
//            checkArgument(ext.matches("[a-zA-Z]+"), "illegal extension:", ext);
//            activeExtensions.add(ext);
//            var patList = mPatternBank.get(ext);
//            if (patList == null) {
//              patList = arrayList();
//              mPatternBank.put(ext, patList);
//            }
//          }
//          continue;
//        }
//        checkState(!activeExtensions.isEmpty(), "no active extensions");
//
//
//        var activeOmit = false;
//
//        if (x.startsWith("{")) {
//          var i = 1 + x.indexOf('}');
//          checkArgument(i > 0, "expected '}'", x);
//          var pref = x.substring(1, i - 1);
//          x = x.substring(i);
//          switch (pref) {
//            case "omit":
//              activeOmit = true;
//              break;
//            default:
//              throw badArg("unrecognized command:", x);
//          }
//        }
//
//
//        // If the pattern ends with '(;', replace this suffix with
//        // something that accepts any amount of text following ( that does NOT include
//        // a semicolon, followed by a semicolon.
//        var y = chomp(x, "(;");
//        if (y != x)
//          x = y + "\\x28[^;]*;";
//
//        for (var ext : activeExtensions) {
//          var patList = mPatternBank.get(ext);
//          var rec = new PatternRecord(x, activeOmit);
//          patList.add(rec);
//        }
//      }
//    }
//    return mPatternBank;
//  }

  private static class PatternRecord {
    PatternRecord(String description /*, boolean omitFlag*/) {
//      this.omitFlag = omitFlag;
      this.description = description;
      this.pattern = RegExp.pattern(description);
    }

    //    boolean omitFlag;
    Pattern pattern;
    String description;

    @Override
    public String toString() {
      var s = "'" + description + "'";
//      if (omitFlag)
//        s += " {omit}";
      return s;
    }
  }

  private Map<String, List<PatternRecord>> mPatternBank;

  /**
   * Construct a DirWalk for a directory.  It will walk through all supported source file types
   */
  private DirWalk getWalker(File dir) {
    return new DirWalk(dir);
  }

//  private List<PatternRecord> patternsForFile(File f) {
//    var ext = Files.getExtension(f);
//    return getPatternBank().get(ext);
//  }

  private static String FILTER_FILENAME = ".filter";

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
    for (var line : parseLinesFromTextFile(content)) {
      todo("do something with filter file line: " + line);
    }
    return currentState;
  }

  private void doSave() {
    int modifiedFilesWithinProject = 0;

    var initialState = prepareState();

    List<DirStackEntry> dirStack = arrayList();
    dirStack.add(new DirStackEntry(initialState, projectDir()));
    while (!dirStack.isEmpty()) {
      var entry = pop(dirStack);

      var filterFile = new File(entry.directory, FILTER_FILENAME);
      if (filterFile.exists()) {
        var content = files().readString(filterFile);
        var newFilterState = processFilterFile(content, entry.filterState);
        entry = entry.withState(newFilterState);
      }

      var w = new DirWalk(entry.directory).withRecurse(false).includeDirectories().omitNames(".DS_Store");
      for (var sourceFileOrDir : w.files()) {
        var name = Files.basename(sourceFileOrDir);
        pr("...proc stack, file:", sourceFileOrDir);
        // Ignore filter filenames
        if (name.equals(FILTER_FILENAME)) {
          todo("we actually want to DELETE these files");
          continue;
        }

        todo("handle file: maybe delete, maybe perform substitutions");

        if (!sourceFileOrDir.isDirectory()) {
          var sourceFile = sourceFileOrDir;
          var ext = Files.getExtension(sourceFile);
          var patterns = entry.filterState.patterns().optListForExtension(ext);
          if (!patterns.isEmpty()) {
            log("file:", w.rel(sourceFile));
            var currText = Files.readString(sourceFile);
            applyFilter(currText, patterns);

            if (mOmitFileFlag || mMatchesWithinFile != 0) {
              modifiedFilesWithinProject++;
              var rel = w.rel(sourceFile);
              var dest = new File(getSaveDir(), rel.toString());
              log("...match found:", INDENT, rel);
              log("...saving to:", INDENT, dest);
              files().mkdirs(Files.parent(dest));
              files().copyFile(sourceFile, dest);

              if (mOmitFileFlag) {
                log("...filtering entire file:", rel);
                files().deleteFile(sourceFile);
              } else {
                // Write new filtered form
                var filteredContent = mNewText.toString();
                log("...writing filtered version of:", rel, INDENT, filteredContent);
                files().writeString(sourceFile, filteredContent);
              }
            }

          }
        } else {
          //      var patterns = patternsForFile(sourceFile);
//      if (patterns == null) continue;
//      log("file:", w.rel(sourceFile));
//      var currText = Files.readString(sourceFile);
//      applyFilter(currText, patterns);
//
//      if (mOmitFileFlag || mMatchesWithinFile != 0) {
//        modifiedFilesWithinProject++;
//        var rel = w.rel(sourceFile);
//        var dest = new File(getSaveDir(), rel.toString());
//        log("...match found:", INDENT, rel);
//        log("...saving to:", INDENT, dest);
//        files().mkdirs(Files.parent(dest));
//        files().copyFile(sourceFile, dest);
//
//        if (mOmitFileFlag) {
//          log("...filtering entire file:", rel);
//          files().deleteFile(sourceFile);
//        } else {
//          // Write new filtered form
//          var filteredContent = mNewText.toString();
//          log("...writing filtered version of:", rel, INDENT, filteredContent);
//          files().writeString(sourceFile, filteredContent);
//        }
//      }
          push(dirStack, entry.withDirectory(sourceFileOrDir));
        }
      }
    }

//    var w = getWalker(projectDir());
//
//    for (var sourceFile : w.files()) {
//      var patterns = patternsForFile(sourceFile);
//      if (patterns == null) continue;
//      log("file:", w.rel(sourceFile));
//      var currText = Files.readString(sourceFile);
//      applyFilter(currText, patterns);
//
//      if (mOmitFileFlag || mMatchesWithinFile != 0) {
//        modifiedFilesWithinProject++;
//        var rel = w.rel(sourceFile);
//        var dest = new File(getSaveDir(), rel.toString());
//        log("...match found:", INDENT, rel);
//        log("...saving to:", INDENT, dest);
//        files().mkdirs(Files.parent(dest));
//        files().copyFile(sourceFile, dest);
//
//        if (mOmitFileFlag) {
//          log("...filtering entire file:", rel);
//          files().deleteFile(sourceFile);
//        } else {
//          // Write new filtered form
//          var filteredContent = mNewText.toString();
//          log("...writing filtered version of:", rel, INDENT, filteredContent);
//          files().writeString(sourceFile, filteredContent);
//        }
//      }
//    }

    if (modifiedFilesWithinProject == 0) {
      setError("No filter matches found... did you mean to do a restore instead?");
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
    var w = getWalker(restDir);
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

  private void applyFilter(String currText, List<PatternRecord> patterns) {

    mNewText = new StringBuilder(currText);

    mMatchesWithinFile = 0;
    mOmitFileFlag = false;

    // Apply each of the patterns
    var patIndex = INIT_INDEX;
    for (var p : patterns) {
      patIndex++;
      var m = p.pattern.matcher(currText);
      while (m.find()) {
//
//        if (p.omitFlag) {
//          mOmitFileFlag = true;
//          return;
//        }

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

  private boolean mOmitFileFlag;
  private int mMatchesWithinFile;
  private StringBuilder mNewText;

  private File findPatternFile() {
    var patFile = config().patternFile();

    if (!config().skipPatternSearch()) {

      // If the configuration value was missing,
      // look a) in the current directory,
      // and  b) in the home directory

      if (Files.empty(patFile)) {
        var c = new File(".prep_patterns.txt");
        if (c.exists()) patFile = c;
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

      log(VERT_SP, "parsing pattern line:", INDENT, x);
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
//          var patList = mPatternBank.get(ext);
//          if (patList == null) {
//            patList = arrayList();
//            mPatternBank.put(ext, patList);
//          }
        }
        continue;
      }


//      var activeOmit = false;
//
//      if (x.startsWith("{")) {
//        var i = 1 + x.indexOf('}');
//        checkArgument(i > 0, "expected '}'", x);
//        var pref = x.substring(1, i - 1);
//        x = x.substring(i);
//        switch (pref) {
//          case "omit":
//            activeOmit = true;
//            break;
//          default:
//            throw badArg("unrecognized command:", x);
//        }
//      }


      // If the pattern ends with '(;', replace this suffix with
      // something that accepts any amount of text following ( that does NOT include
      // a semicolon, followed by a semicolon.
      var y = chomp(x, "(;");
      if (y != x)
        x = y + "\\x28[^;]*;";

      for (var ext : activeExtensions) {
//        var patList = mPatternBank.get(ext);
        var rec = new PatternRecord(x);//, activeOmit);
        patterns.add(ext, rec);
//        patList.add(rec);
      }

    }

    var state = new FilterState(patterns);
    return state;
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

    public void add(String extension, PatternRecord pattern) {
      var x = getListForExtension(extension);
      x.add(pattern);
      todo("!check for duplicate patterns");
    }

    private List<PatternRecord> getListForExtension(String extension) {
      var x = mPatternBank.get(extension);
      if (x == null) {
        x = arrayList();
        mPatternBank.put(extension, x);
      }
      return x;
    }

    public List<PatternRecord> optListForExtension(String extension) {
      var x = mPatternBank.get(extension);
      if (x == null)
        return DataUtil.emptyList();
      return x;
    }

    private Map<String, List<PatternRecord>> mPatternBank;
  }

  private static class FilterState {
    public FilterState(PatternCollection patterns) {
      mPatterns = patterns;
    }

    public PatternCollection patterns() {
      return mPatterns;
    }

    // This map should be considered immutable.  If changes are made, construct a new copy
    private PatternCollection mPatterns;

//    private Map<String, List<PatternRecord>> mPatternBank;
  }
}
