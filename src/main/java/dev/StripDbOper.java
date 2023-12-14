package dev;

import static js.base.Tools.*;

import java.util.List;
import java.util.regex.Pattern;

import dev.gen.StripDbConfig;
import js.app.AppOper;
import js.file.BackupManager;
import js.file.Files;
import js.parsing.RegExp;

public class StripDbOper extends AppOper {

  @Override
  public String userCommand() {
    return "stripdb";
  }

  @Override
  public String getHelpDescription() {
    return "no help is available for operation";
  }

  @Override
  public StripDbConfig defaultArgs() {
    return StripDbConfig.DEFAULT_INSTANCE;
  }

  @Override
  public StripDbConfig config() {
    if (mConfig == null) {
      mConfig = (StripDbConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {

    var path = config().file();
    path = Files.addExpectedExtension(Files.assertNonEmpty(path), "java");
    Files.assertExists(path, "file to modify");

    var b = new BackupManager(Files.S, Files.absolute(path).getParentFile());

    List<Pattern> patterns = arrayList();
    for (var expr : config().exprs()) {
      patterns.add(RegExp.pattern("[\\t ]+" + expr + "\\("));
    }

    String content = Files.readString(path);
    String origContent = content;

    boolean modified = false;

    outer: do {
      modified = false;
      for (var p : patterns) {
        var m = p.matcher(content);
        int cursor = 0;
        while (true) {
          if (!m.find(cursor))
            break;
          int start = m.start();
          int end = m.end();
          cursor = end;

          var location = expandMatch(content, start, end);
          if (location != null) {
            content = content.substring(0, location[0]) + content.substring(location[1]);
            modified = true;
            continue outer;
          }
        }
      }
    } while (modified);

    if (!content.equals(origContent)) {
      log("...updating:", path);
      b.makeBackup(path);
      Files.S.writeString(path, content);
    }
  }

  private int[] expandMatch(String s, int start, int end) {
    int[] result = new int[2];
    while (true) {
      var s2 = start - 1;
      if (s2 < 0)
        break;
      char c = s.charAt(s2);
      if (c == '\n' || c > ' ')
        break;
      start = s2;
    }

    // Look for first linefeed after semicolon
    int state = 0;
    while (true) {
      if (end == s.length())
        break;
      char c = s.charAt(end);
      end++;
      // If it is a function body, ignore this match
      if (state == 0 && c == '{') {
        return null;
      }
      if (state == 0 && c == ';') {
        state++;
      }
      if (state == 1 && c == '\n') {
        break;
      }
    }
    log("match at:", INDENT, quote(s.substring(start, end)));
    result[0] = start;
    result[1] = end;
    return result;
  }

  private StripDbConfig mConfig;

}
