package dev.strip;

import static js.base.Tools.*;

import dev.gen.EditCode;
import dev.gen.StripConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.json.JSUtils;
import js.parsing.DFA;
import js.parsing.DFACache;
import js.parsing.Lexer;

import java.io.File;
import java.util.*;

public class StripOper extends AppOper {

  // If present within a directory, a list of files (or directories) to delete from that directory (within the target branch)
  public static final String DELETE_FILES_LIST = ".delete";

  // If present within a directory, uses this list of files (or directories) to examine;
  // otherwise, examines all of them
  public static final String EXPLICIT_FILES_LIST = ".files";

  // If there is a file with this name in the project root directory, it defines the regular expressions
  // to apply the filter to.  If missing, the default file will be used
  public static final String PROJECT_INFO_FILE = ".strip_project";

  public static final String STRIP_OPER_ARGS_FILE = "strip-args.json";


  @Override
  public String userCommand() {
    return "strip";
  }

  @Override
  public String shortHelp() {
    return "copy source branch to target, stripping out development code";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("** this help is out of date**[ pattern_file <hhhhhhh> ]", "file describing deletion patterns");
    b.pr(hf);
    b.br();
  }

  @Override
  public StripConfig defaultArgs() {
    return StripConfig.DEFAULT_INSTANCE;
  }

  @Override
  public StripConfig config() {
    if (mConfig == null) {
      mConfig = super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var c = config();

    if (c.defaults()) {
      pr(defaultExpressionsContent());
      return;
    }

    if (!inTestMode()) {
      var b = currentGitBranch();
      var pref = "dev-";
      if (!b.startsWith(pref)) {
        var desired = "dev-" + b;
        pr("*** current git branch is:", quote(b), "; attempting to switch to:", quote(desired));

        var sc = new SystemCall();
        sc.withVerbose(verbose());
        sc.arg("git", "checkout", desired);
        sc.assertSuccess();
        pr("... switched, and quitting.  Rerun if desired.");
        return;
      }
    }

    checkArgument(nonEmpty(c.sourceBranch()), "source_branch is empty");
    checkArgument(nonEmpty(c.targetBranch()), "target_branch is empty");

    files().withDryRun(dryRun());
    log("arguments:", INDENT, config());
    log("Project directory:", projectDir());
    log("Cache directory:", cacheDir());
    doStrip();
  }

  /**
   * Determine the project directory
   */
  private File projectDir() {
    var x = mCachedProjectDir;
    if (x == null) {
      x = config().projectDir();
      if (Files.empty(x)) {
        // Look for project directory
        x = Files.currentDirectory();
        while (true) {
          if (Files.join(x, ".git").exists()) {
            break;
          }
          x = Files.parent(x);
        }
      }
      mCachedProjectDir = x;
    }

    if (x.toString().endsWith("/Users/jeff/github_projects/dev")) {
      die("WTF, shouldn't be operating on our own source directory");
    }

    return x;
  }

  private void selectSourceBranch() {
    auxSelectBranch(0);
  }

  private void selectTargetBranch() {
    auxSelectBranch(1);
  }

  private boolean inTestMode() {
    return config().sourceBranch().startsWith("$");
  }

  public static final String TESTING_DIR_SUFFIX = "_target";

  private void auxSelectBranch(int index) {
    if (inTestMode()) {
      var f = projectDir();
      var p = Files.parent(f);
      var nm = f.getName();
      nm = chomp(nm, TESTING_DIR_SUFFIX);
      if (index == 1)
        nm = nm + TESTING_DIR_SUFFIX;
      f = Files.join(p, nm);
      mCachedProjectDir = f;
    } else {
      var sc = new SystemCall();
      sc.withVerbose(verbose());
      var br = (index == 0) ? config().sourceBranch() : config().targetBranch();
      sc.arg("git", "checkout", br);
      sc.assertSuccess();
    }
  }

  private File mCachedProjectDir;

  private String projectInfoFileContent() {
    var content = mCachedProjectInfoFileContent;
    if (content == null) {
      var infoFile = Files.join(projectDir(), PROJECT_INFO_FILE);
      if (infoFile.exists()) {
        content = Files.readString(infoFile);
      } else {
        content = defaultExpressionsContent();
      }
      mCachedProjectInfoFileContent = content;
    }
    return content;
  }

  private String mCachedProjectInfoFileContent;

  private String defaultExpressionsContent() {
    return Files.readString(this.getClass(), "strip_default.txt");
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

  private FilterState processDeleteList(FilterState current, String content) {
    Set<File> deleteFiles = hashSet();
    for (var line : parseLinesFromTextFile(content)) {
      deleteFiles.add(Files.join(current.directory(), line));
    }
    return new FilterState(current.directory(), deleteFiles);
  }


  public /* for tests */ static final List<String> ALWAYS_DELETE_THESE_FILES = arrayList(DELETE_FILES_LIST, PROJECT_INFO_FILE, EXPLICIT_FILES_LIST,
      STRIP_OPER_ARGS_FILE);

  private void doStrip() {
    selectSourceBranch();
    var editsMap = generateEditsMap();
    selectTargetBranch();
    processEditsMap(editsMap);
    if (dryRun()) {
      selectSourceBranch();
    }
  }

  private JSMap generateEditsMap() {
    var initialState = prepareState();
    List<FilterState> dirStack = arrayList();
    dirStack.add(initialState);

    while (!dirStack.isEmpty()) {
      var state = pop(dirStack);

      // If there's a .delete list, parse it and add those files to the delete list
      var deleteListFile = new File(state.directory(), DELETE_FILES_LIST);
      if (deleteListFile.exists()) {
        var content = Files.readString(deleteListFile);
        state = processDeleteList(state, content);
      }

      // Examine the files (or subdirectories) within this directory (without recursing).
      // If an explicit file list exists, parse that for the list of files instead.
      // The result is a list of files or directories, relative to the current entry's directory

      var listOfFiles = constructFilesWithinDirAbs(state.directory());

      //pr("list of files:", niceList(listOfFiles));

      for (var abs : listOfFiles) {

        if (!config().includeSymlinks()) {
          // If the file (or dir) is a symlink, don't process it
          if (isSymLink(abs))
            continue;
        }

        var relativeToProject = Files.relativeToContainingDirectory(abs, projectDir());

        if (ALWAYS_DELETE_THESE_FILES.contains(abs.getName()) || state.deleteFilesAbs().contains(abs)) {
          log("..........filtering entire file or dir:", relativeToProject);
          recordEdit(relativeToProject, EditCode.DELETE, "");
          continue;
        }

        if (!abs.isDirectory()) {
          var editMade = false;
          var ext = Files.getExtension(abs);
          var dfa = dfaForExtension(ext);
          String currText = null;
          if (dfa != null) {
            log("file:", relativeToProject);
            currText = Files.readString(abs);
            applyFilter(currText, dfa, verbose());
            if (mMatchesWithinFile != 0) {
              editMade = true;
              currText = mNewText.toString();
            }
          }

          if (!editMade && !fileWithinExcludeList(abs)) {
            editMade = true;
          }

          if (editMade) {
            if (currText == null)
              currText = Files.readString(abs);
            recordEdit(relativeToProject, EditCode.MODIFY, currText);
          }
        } else {
          var newEnt = state.descendInto(abs);
          log("...descending into dir");
          push(dirStack, newEnt);
        }
      }
    }
    return mEditsMap;
  }

  /**
   * Get list of files (or subdirectories) within a directory to extend the filter traversal to.
   * If an explict file list exists, parse that for the list of files instead.
   */
  private List<File> constructFilesWithinDirAbs(File dir) {
    var explicitFileList = new File(dir, EXPLICIT_FILES_LIST);
    if (explicitFileList.exists()) {
      Set<File> setOfFiles = hashSet();
      var content = Files.readString(explicitFileList);
      for (var line : parseLinesFromTextFile(content)) {
        var candidateFile = new File(dir, line);
        if (candidateFile.exists())
          setOfFiles.add(candidateFile);
      }
      List<File> listOfFiles = arrayList();
      for (var x : setOfFiles) {
        if (x.exists()) {
          listOfFiles.add(x);
        }
      }
      log("explicit files list encountered, content:", INDENT, content);
      log("resulting files:", INDENT, niceList(listOfFiles));
      return listOfFiles;
    } else {
      var walk = new DirWalk(dir).withRecurse(false).includeDirectories() //
          .omitPrefixes(".") // also omits .DS_Store
          ;
      return walk.files();
    }
  }

  private static boolean isSymLink(File f) {
    return !f.getAbsoluteFile().equals(Files.getCanonicalFile(f));
  }

  public static JSMap niceList(Collection<File> lst) {
    var m = map();
    for (var f : lst) {
      m.putNumbered(f.getName());
    }
    return m;
  }

  public static JSMap niceStringList(Collection<String> lst) {
    var m = map();
    for (var f : lst) {
      m.putNumbered(f);
    }
    return m;
  }

  private static final char ERASE_CHAR = 0x7f;

  private void applyFilter(String currText, DFA dfa, boolean verbose) {

    var filteredText = new StringBuilder();
    mMatchesWithinFile = 0;

    var s = new Lexer(dfa).withText(currText).withNoSkip().withAcceptUnknownTokens();

    while (s.hasNext()) {
      var tk = s.read();
      var tkText = tk.text();
      if (verbose) {
        log("=== token:", dfa.tokenName(tk.id()), JSUtils.valueToString(tkText));
      }
      var tokenTextLength = tkText.length();

      if (tk.isUnknown()) {
        // This text is to be left alone
        filteredText.append(tkText);
      } else {
        if (verbose) log("...found matching token");
        mMatchesWithinFile++;


        var tokenName = dfa.tokenName(tk.id());
        if (tokenName.startsWith("ALTERNATIVE")) {
          procExcludeToken(tkText, filteredText);
        } else {
          erase(filteredText, tokenTextLength);
        }
      }
    }

    mNewText = filteredText;
    if (mMatchesWithinFile != 0)
      pass2();
  }


  private static void erase(StringBuilder target, int len) {
    target.append(String.valueOf(ERASE_CHAR).repeat(len));
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
        out.append(spaces(bufferedSpaceCount));
        bufferedSpaceCount = 0;
        out.append(c);
      }
    }
    mNewText.setLength(0);
    mNewText.append(out);
  }

  private StripConfig mConfig;
  private File mCacheDir;

  private int mMatchesWithinFile;
  private StringBuilder mNewText;


  /**
   * Set up the initial filter state.
   *
   * We also parse the project info file (if one exists), constructing the .rxp files for each supported file extension.
   * Then we regenerate the .dfa files from those .rxp files (if they aren't cached already)
   */
  private FilterState prepareState() {
    var text = projectInfoFileContent();
    Set<String> activeExtensions = hashSet();

    for (var x : split(text, '\n')) {

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

      for (var ext : activeExtensions) {
        var buffer = extensionBuffer(ext);
        buffer.append(x);
        buffer.append('\n');
      }

    }

    // Construct DFAs from each extension
    {
      var v = verbose() && false;
      var dfaCache = DFACache.SHARED_INSTANCE;
      dfaCache.withCacheDir(Files.getDesktopFile("_prepoper_dfa_cache_"));
      if (v)
        dfaCache.setVerbose();

      log("Constructing dfas from rxp", CR, DASHES);
      for (var ent : mRXPContentForFileExtensionMap.entrySet()) {
        String ext = ent.getKey();
        String rxp = ent.getValue().toString();
        if (v) log("Constructing DFA for extension:", ext);
        if (v) log("rxp file:", INDENT, rxp);
        var dfa = dfaCache.forTokenDefinitions(rxp);
        if (v) {
          log("dfa file:", INDENT, dfa.toJson().remove("graph"));
          log(DASHES);
        }
        mDFAForFileExtensionMap.put(ext, dfa);
      }
    }

    return new FilterState(projectDir(), arrayList());
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

  private String currentGitBranch() {
    constructGitInfo();
    return mCurrentGitBranch;
  }

  private void discardGitInfo() {
    mGitBranches = null;
    mCurrentGitBranch = null;
  }

  private void constructGitInfo() {
    var x = mGitBranches;
    if (x == null) {
      var sc = new SystemCall();
      sc.arg("git", "branch");
      var res = sc.systemOut();
      List<String> lines = arrayList();
      String currentBranch = "";
      for (var y : split(res, '\n')) {
        y = y.trim();
        if (y.isEmpty()) continue;
        var trimmed = chompPrefix(y, "*").trim();
        if (trimmed.length() < y.length()) {
          currentBranch = trimmed;
        }
        lines.add(trimmed);
      }
      checkState(currentBranch != null, "can't determine current branch");
      x = lines;
      mCurrentGitBranch = currentBranch;
      mGitBranches = x;
    }
  }

  private List<String> mGitBranches;
  private String mCurrentGitBranch;

  private Map<String, DFA> mDFAForFileExtensionMap = hashMap();

  private void recordEdit(File relFile, EditCode code, String content) {
    var s = code + "::" + content;
    mEditsMap.put(relFile.toString(), s);
  }

  private JSMap mEditsMap = map();


  private boolean fileWithinExcludeList(File f) {
    var name = f.getName();
    var s = mExcludeExtensionsSet;
    if (s == null) {
      s = hashSet();
      s.addAll(split(
          config().excludeExtensions(), ','));
      mExcludeExtensionsSet = s;
    }
    return s.contains(name);

  }

  private Set<String> mExcludeExtensionsSet;

  private void processEditsMap(JSMap editsMap) {
    if (dryRun()) {
      if (!inTestMode()) {
        pr("dry run; edits map:", INDENT, editsMap);
        var editsMapSuccinct = map();
        for (var key : editsMap.keySet()
        ) {
          var value = editsMap.get(key);
          var i = value.indexOf("::");
          if (i > 0)
            value = value.substring(0, i);
          editsMapSuccinct.put(key, value);
        }
        pr("succinct version:", INDENT, editsMapSuccinct);
      }
      return;
    }
    for (var relPath : editsMap.keySet()) {
      var targetFile = new File(projectDir(), relPath);
      var arg = editsMap.get(relPath);
      var i = arg.indexOf("::");
      var cmd = arg.substring(0, i);
      var content = arg.substring(i + 2);

      var ec = EditCode.valueOf(cmd);
      switch (ec) {
        default:
          throw notSupported("edit code:", ec, "for:", arg);
        case DELETE:
          if (targetFile.exists()) {
            if (targetFile.isDirectory()) {
              files().deleteDirectory(targetFile);
            } else {
              files().deleteFile(targetFile);
            }
          }
          break;
        case MODIFY:
          files().writeString(targetFile, content);
          break;
      }
    }
  }


  private static int startOfLineContaining(String text, String pattern) {
    var i = text.indexOf(pattern);
    if (i >= 0) {
      i += pattern.length();
      while (i > 0 && text.charAt(i - 1) != '\n')
        i--;
    }
    return i;
  }

  private static int endOfLineContaining(String text, String pattern) {
    var i = text.indexOf(pattern);
    if (i >= 0) {
      i += pattern.length();
      while (i < text.length() && text.charAt(i - 1) != '\n')
        i++;
    }
    return i;
  }

  public static void procExcludeToken(String tokenText, StringBuilder filterDestination) {
    var extraLogging = false && tokenText.contains("~|~");
    if (extraLogging)
      pr(VERT_SP, "filtering ALTERNATIVE:", INDENT, quote(tokenText));
    int altLoc = endOfLineContaining(tokenText, "~|~");
    if (altLoc < 0) {
      if (extraLogging) pr("...no ALTERNATIVE, erasing entire token");
      erase(filterDestination, tokenText.length());
    } else {
      int loc2 = startOfLineContaining(tokenText, "~}");
      checkState(loc2 >= altLoc);
      if (extraLogging) pr("...ALTERNATIVE, retaining", altLoc, "...", loc2, quote(tokenText.substring(altLoc, loc2)));

      erase(filterDestination, altLoc);
      //checkState(tokenText.equals(tokenText.substring(altLoc, loc2)));
      filterDestination.append(tokenText.substring(altLoc, loc2));
      erase(filterDestination, tokenText.length() - loc2);
    }
  }

}

