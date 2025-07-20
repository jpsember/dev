package dev;

import static js.base.Tools.*;

import js.base.BaseObject;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;
import js.geometry.MyMath;
import js.parsing.DFA;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public final class DfaCache extends BaseObject {

  private static final String DFA_EXT = "dfa";

  public static DfaCache SHARED_INSTANCE = new DfaCache();

  private DfaCache() {
  }

  public synchronized DFA forTokenDefinitions(String rxpScript) {

    String dfaKey = UUID.nameUUIDFromBytes(rxpScript.getBytes()).toString();
    var result = mMemoryMap.get(dfaKey);
    log("get DFA from key:", dfaKey);
    if (result != null) {
      return result;
    }
    log("...not in cache; building; source:", INDENT, rxpScript);

    var dfaFile = new File(cacheDir(), dfaKey + "." + DFA_EXT);

    if (!dfaFile.exists()) {
      // Trim the cache to a reasonable size
      final int MAX_CACHE_SIZE = 20;
      {
        var dw = new DirWalk(cacheDir()).withExtensions(DFA_EXT);
        var dfaList = dw.files();
        if (dfaList.size() > MAX_CACHE_SIZE) {
          MyMath.permute(dfaList, null);
          while (dfaList.size() > MAX_CACHE_SIZE) {
            var purgeFile = pop(dfaList);
            log("...culling cache, removing:", INDENT, purgeFile);
            files().deleteFile(purgeFile);
          }
        }
      }

      // Call the dfa program to compile regexps to a dfa
      var tmpDir = Files.createTempDir("PrepOper_build_dfa");
      var inputFile = new File(tmpDir, "x.rxp");
      files().writeString(inputFile, rxpScript);
      var sc = new SystemCall().withVerbose(verbose());
      sc.arg("dfa", "input", inputFile, "output", dfaFile);
      sc.assertSuccess();
    }
    var dfa = DFA.parse(Files.readString(dfaFile));
    mMemoryMap.put(dfaKey, dfa);
    return dfa;
  }

  public void withCacheDir(File dir) {
    Files.assertNonEmpty(dir, "cache dir");
    files().mkdirs(dir);
    mCacheDir = dir;
  }

  private File cacheDir() {
    if (mCacheDir == null) {
      withCacheDir(new File(Files.homeDirectory(), ".dfa_cache"));
    }
    return mCacheDir;
  }

  private Files files() {
    return Files.S;
  }

  private File mCacheDir;
  private Map<String, DFA> mMemoryMap = hashMap();
}
