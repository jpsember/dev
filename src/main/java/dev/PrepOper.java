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
  private static final boolean SINGLE_SET = true;

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
    alertVerbose();
    log("Project directory:", projectDir());
    log("Cache directory:", cacheDir());
    log("Saving:", saving(), "Restoring:", restoring());

    if (saving()) {
      doSave();
    } else {
      doRestore();
    }
  }

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
        var gitDir = new File(cursor, ".git");
        if (gitDir.isDirectory()) {
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

  private File cacheDir() {
    if (mCacheDir == null) {
      var p = projectDir().toString();
      p = p.replaceAll("[\\x3a\\x5c\\x2f\\x20]", "_");
      log("cache dir:", projectDir(), "=>", p);
      mCacheDir = Files.getDesktopFile(".prep_oper_cache/" + p);
      files().mkdirs(mCacheDir);
    }
    return mCacheDir;
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

  private static boolean isSpaceOrTab(char c) {
    return (c == ' ' || c == (char) 0x09);
  }

  private int[] extendWithinWhiteSpace(String text, int a, int b) {
    while (a - 1 >= 0 && isSpaceOrTab(text.charAt(a - 1)))
      a--;
    while (b < text.length() && isSpaceOrTab(text.charAt(b))) b++;
    return new int[]{a, b};
  }

  private void doSave() {
    int modifiedFilesWithinProject = 0;
    var w = new DirWalk(projectDir()).withExtensions("java", "rs");
    for (var f : w.files()) {

      log("file:", w.rel(f));
      var text = Files.readString(f);

      var newText = new StringBuilder(text);

      int matchesWithinFile = 0;

      // Apply each of the patterns
      for (var p : patterns()) {

        var m = p.matcher(text);
        while (m.find()) {
          matchesWithinFile++;

          var start = m.start();
          var end = m.end();

          // Replace all non-linefeed characters from start to end with spaces.
          // Do this in the new text buffer, not the one we're matching within.
          for (int j = start; j < end; j++) {
            char c = newText.charAt(j);
            if (c != '\n')
              newText.setCharAt(j, ' ');
          }

//          // If the matching text is alone on its line (except for whitespace),
//          // delete the whole line; otherwise, just the text itself
//          var wsExtent = extendWithinWhiteSpace(text, start, end);
//          var a = wsExtent[0];
//          var b = wsExtent[1];
//
//          var deleteLine = false;
//          var trimLeft = start;
//          var trimRight = end;
//          {
//            if ((a - 1 < 0 || text.charAt(a - 1) == '\n')  && (b == text.length() || text.charAt(b) == '\n')) {
//              trimLeft = a;
//              trimRight = b;
//              deleteLine = true;
//            }
//          }
//
//          if (deleteLine) {
//
//          }
//          // find start of line containing start, and end of line containing end
//          int j = start;
//          boolean startLineFlag = false;
//          while (true) {
//            if (j - 1 < 0) {
//              startLineFlag = true;
//              break;
//            }
//            char c = text.charAt(j - 1);
//            if (!(c == 0x20 || c == 0x09)) {
//              if (c == '\n') startLineFlag = true;
//              break;
//            }
//            j--;
//          }
//          boolean endLineFlag = false;
//          int k = end;
//          while (true) {
//            if (k >= text.length()) {
//              endLineFlag = true;
//              break;
//            }
//            char c = text.charAt(k);
//            if (!(c == 0x20 || c == 0x09)) {
//              if (c == '\n') endLineFlag = true;
//              break;
//            }
//            k++;
//          }

        }
      }


      if (matchesWithinFile != 0) {
        modifiedFilesWithinProject++;
        var rel = w.rel(f);
        var dest = new File(getSaveDir(), rel.toString());
        log("...match found:", INDENT, rel);
        log("...saving to:", INDENT, dest);
        files().mkdirs(Files.parent(dest));
        files().copyFile(f, dest);

        // Write new filtered form
        var filteredContent = newText.toString();
        log("...writing filtered version of:", rel, INDENT, filteredContent);
        files().writeString(f, filteredContent);
      }
    }

    if (modifiedFilesWithinProject == 0) {
      setError("No filter matches found... did you mean to do a restore instead?");
    }
  }

  private File getSaveDir() {
    if (mSaveDir == null) {
      pr(VERT_SP, "************ CREATING NEW SAVE DIR", VERT_SP);
      var w = new DirWalk(cacheDir()).withRecurse(false).includeDirectories();
      List<File> found = arrayList();

      for (var f : w.files()) {
        if (f.isDirectory()) {
          if (false && alert("deleting all existing")) {
            files().deleteDirectory(f, "prep_oper_cache");
            continue;
          }
          found.add(f);
        }
      }
      found.sort(Files.COMPARATOR);
      log("found backup dirs:", INDENT, found);

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
    throw notFinished();
  }

  private List<Pattern> patterns() {
    if (mPatterns == null) {
      List<Pattern> p = arrayList();
      var text = Files.readString(this.getClass(), "prep_default.txt");
      for (var x : split(text, '\n')) {
        x = x.trim();
        if (x.startsWith("#")) continue;
        if (x.isEmpty()) continue;
        var expr = RegExp.pattern(x);
        p.add(expr);
      }
      mPatterns = p;
    }
    return mPatterns;
  }

  private List<Pattern> mPatterns;
  private PrepConfig mConfig;
  private File mProjectDir;
  private File mCacheDir;
  private Boolean mSaving;
}
