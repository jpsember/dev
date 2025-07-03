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

  private void doSave() {

    int modFilesCount = 0;

    var w = new DirWalk(projectDir()).withExtensions("java", "rs");
    for (var f : w.files()) {

      log("file:", w.rel(f));
      var text = Files.readString(f);

      int matchCount = 0;
      var sb = new StringBuilder();
      for (var x : split(text, '\n')) {
        boolean include = true;
        for (var p : patterns()) {
          var m = p.matcher(x);
          if (!m.find()) continue;
          include = false;
          break;
        }
        if (include) {
          sb.append(x);
          sb.append('\n');
        } else {
          if (matchCount == 0)
            modFilesCount++;
          matchCount++;
        }
      }

      if (matchCount != 0) {
        var rel = w.rel(f);
        var dest = new File(getSaveDir(), rel.toString());
        log("...match found:", INDENT, rel);
        log("...saving to:", INDENT, dest);
        files().mkdirs(Files.parent(dest));
        files().copyFile(f, dest);

        // Write new filtered form
        var filteredContent = sb.toString();
        log("...writing filtered version of:", rel, INDENT, filteredContent);
       files().writeString(f, filteredContent);
      }
    }

    if (modFilesCount == 0) {
      setError("No filter matches found... did you mean to do a restore instead?");
    }
  }

  private File getSaveDir() {
    if (mSaveDir == null) {
pr(VERT_SP,"************ CREATING NEW SAVE DIR",VERT_SP);
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

      while (found.size() > MAX_BACKUP_SETS) {
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
