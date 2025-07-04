package dev;

import static js.base.Tools.*;

import dev.gen.PrepConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.RegExp;

import java.io.File;
import java.util.List;
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
    hf.addItem(" ...nothing yet...", "xxx");
    b.pr(hf);
    b.br();
    b.pr("...nothing yet...");
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

  /**
   * Construct a DirWalk for a directory.  It will walk through all supported source file types
   */
  private DirWalk getWalker(File dir) {
    return new DirWalk(dir).withExtensions(split(config().sourceFileExtensions(), ' '));
  }

  private void doSave() {
    int modifiedFilesWithinProject = 0;

    var w = getWalker(projectDir());

    for (var sourceFile : w.files()) {

      log("file:", w.rel(sourceFile));
      var currText = Files.readString(sourceFile);

      var newText = applyFilter(currText);


      if (mMatchesWithinFile != 0) {
        modifiedFilesWithinProject++;
        var rel = w.rel(sourceFile);
        var dest = new File(getSaveDir(), rel.toString());
        log("...match found:", INDENT, rel);
        log("...saving to:", INDENT, dest);
        files().mkdirs(Files.parent(dest));
        files().copyFile(sourceFile, dest);

        if (mDeleteFileFlag) {
          log("...filtering entire file:", rel);
          files().deleteFile(sourceFile);
        } else {
          // Write new filtered form
          var filteredContent = newText.toString();
          log("...writing filtered version of:", rel, INDENT, filteredContent);
          files().writeString(sourceFile, filteredContent);
        }
      }
    }

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

  private List<Pattern> patterns() {
    if (mPatterns == null) {
      List<Pattern> p = arrayList();
      String text;

      var patFile = config().patternFile();
      if (Files.nonEmpty(patFile)) {
        Files.assertExists(patFile, "pattern_file");
        text = Files.readString(patFile);
      } else
        text = Files.readString(this.getClass(), "prep_default.txt");

      for (var x : split(text, '\n')) {
        x = x.trim();
        if (x.startsWith("#")) continue;
        if (x.isEmpty()) continue;

        // If the pattern ends with '(;', replace this suffix with
        // something that accepts any amount of text following ( that does NOT include
        // a semicolon, followed by a semicolon.
        var y = chomp(x, "(;");
        if (y != x)
          x = y + "\\x28[^;]*;";
        p.add(RegExp.pattern(x));
      }
      mPatterns = p;
    }
    return mPatterns;
  }


  private String applyFilter(String currText) {

    // Construct a copy of the source file that we can
    // edit as we handle pattern matches
    //
    var newText = new StringBuilder(currText);

    mMatchesWithinFile = 0;
    mDeleteFileFlag = false;

    // Apply each of the patterns
    var patIndex = INIT_INDEX;
    outer:
    for (var p : patterns()) {
      patIndex++;
      var m = p.matcher(currText);
      while (m.find()) {
        // Make sure the text is either at the start of a line, or
        // is preceded by some whitespace.
        var start = m.start();
        var end = m.end();

        log("...found match for pattern:", p, "at start:", start, "to:", end, "text:", currText.substring(start, end));

        if (!(start == 0 || currText.charAt(start - 1) <= ' ')) {
          log("...pattern does NOT occur at start of line, ignoring");
          continue;
        }

        mMatchesWithinFile++;

        // The first pattern is special: if a match is found, the entire file is deleted.
        //
        if (patIndex == 0) {
          mDeleteFileFlag = true;
          break outer;
        }

        // Replace all non-linefeed characters from start to end with spaces.
        // Do this in the new text buffer, not the one we're matching within.
        for (int j = start; j < end; j++) {
          char c = newText.charAt(j);
          if (c != '\n')
            newText.setCharAt(j, ' ');
        }
      }
    }
    return newText.toString();
  }

  private List<Pattern> mPatterns;
  private PrepConfig mConfig;
  private File mProjectDir;
  private File mCacheDir;
  private Boolean mSaving;

  private boolean mDeleteFileFlag;
  private int mMatchesWithinFile;

}
