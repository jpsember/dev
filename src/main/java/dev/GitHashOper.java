package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.GitHashConfig;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.Files;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHashOper extends AppOper {

  @Override
  public String userCommand() {
    return "githash";
  }

  @Override
  public String getHelpDescription() {
    return "update git hashes";
  }

  @Override
  public GitHashConfig defaultArgs() {
    return GitHashConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {
    mConfig = config();
    File src = Files.assertExists(mConfig.source(), "file to update");
    File repoDirs = Files.assertDirectoryExists(Files.absolute(mConfig.repoDirs()), "repo_dirs");

    mSourceText = Files.readString(src);
    Pattern p = Pattern.compile(mConfig.pattern());
    Matcher m = p.matcher(mSourceText);

    while (m.find()) {
      String depName = m.group(1);
      String currentHash = m.group(2);

      File dir = new File(repoDirs, depName);
      Files.assertDirectoryExists(dir, depName);

      dir = Files.absolute(dir);
      SystemCall sc = new SystemCall();
      sc.directory(dir);
      sc.arg("git", "rev-parse", "--short", "HEAD");
      String newHash = sc.systemOut().trim();

      adv(m.start(2));
      mSb.append(newHash);
      mCursor = m.end();

      if (!newHash.equals(currentHash))
        log("updated hash for:", depName);
    }
    files().writeIfChanged(src, mSb.toString());
  }

  private void adv(int pos) {
    checkArgument(pos >= mCursor);
    if (pos > mCursor) {
      mSb.append(mSourceText.substring(mCursor, pos));
      pos = mCursor;
    }
  }

  private GitHashConfig mConfig;
  private int mCursor;
  private StringBuilder mSb = new StringBuilder();
  private String mSourceText;
}
