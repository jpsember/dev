package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class PrepConfig implements AbstractData {

  public boolean save() {
    return mSave;
  }

  public boolean restore() {
    return mRestore;
  }

  public File projectRoot() {
    return mProjectRoot;
  }

  public File patternFile() {
    return mPatternFile;
  }

  public File cacheDir() {
    return mCacheDir;
  }

  public File cacheFilename() {
    return mCacheFilename;
  }

  public String cachePathExpr() {
    return mCachePathExpr;
  }

  public boolean skipPatternSearch() {
    return mSkipPatternSearch;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "save";
  protected static final String _1 = "restore";
  protected static final String _2 = "project_root";
  protected static final String _3 = "pattern_file";
  protected static final String _4 = "cache_dir";
  protected static final String _5 = "cache_filename";
  protected static final String _6 = "cache_path_expr";
  protected static final String _7 = "skip_pattern_search";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mSave);
    m.putUnsafe(_1, mRestore);
    m.putUnsafe(_2, mProjectRoot.toString());
    m.putUnsafe(_3, mPatternFile.toString());
    m.putUnsafe(_4, mCacheDir.toString());
    m.putUnsafe(_5, mCacheFilename.toString());
    m.putUnsafe(_6, mCachePathExpr);
    m.putUnsafe(_7, mSkipPatternSearch);
    return m;
  }

  @Override
  public PrepConfig build() {
    return this;
  }

  @Override
  public PrepConfig parse(Object obj) {
    return new PrepConfig((JSMap) obj);
  }

  private PrepConfig(JSMap m) {
    mSave = m.opt(_0, false);
    mRestore = m.opt(_1, false);
    {
      mProjectRoot = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mProjectRoot = new File(x);
      }
    }
    {
      mPatternFile = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mPatternFile = new File(x);
      }
    }
    {
      mCacheDir = Files.DEFAULT;
      String x = m.opt(_4, (String) null);
      if (x != null) {
        mCacheDir = new File(x);
      }
    }
    {
      mCacheFilename = _D5;
      String x = m.opt(_5, (String) null);
      if (x != null) {
        mCacheFilename = new File(x);
      }
    }
    mCachePathExpr = m.opt(_6, "");
    mSkipPatternSearch = m.opt(_7, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof PrepConfig))
      return false;
    PrepConfig other = (PrepConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSave == other.mSave))
      return false;
    if (!(mRestore == other.mRestore))
      return false;
    if (!(mProjectRoot.equals(other.mProjectRoot)))
      return false;
    if (!(mPatternFile.equals(other.mPatternFile)))
      return false;
    if (!(mCacheDir.equals(other.mCacheDir)))
      return false;
    if (!(mCacheFilename.equals(other.mCacheFilename)))
      return false;
    if (!(mCachePathExpr.equals(other.mCachePathExpr)))
      return false;
    if (!(mSkipPatternSearch == other.mSkipPatternSearch))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + (mSave ? 1 : 0);
      r = r * 37 + (mRestore ? 1 : 0);
      r = r * 37 + mProjectRoot.hashCode();
      r = r * 37 + mPatternFile.hashCode();
      r = r * 37 + mCacheDir.hashCode();
      r = r * 37 + mCacheFilename.hashCode();
      r = r * 37 + mCachePathExpr.hashCode();
      r = r * 37 + (mSkipPatternSearch ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected boolean mSave;
  protected boolean mRestore;
  protected File mProjectRoot;
  protected File mPatternFile;
  protected File mCacheDir;
  protected File mCacheFilename;
  protected String mCachePathExpr;
  protected boolean mSkipPatternSearch;
  protected int m__hashcode;

  public static final class Builder extends PrepConfig {

    private Builder(PrepConfig m) {
      mSave = m.mSave;
      mRestore = m.mRestore;
      mProjectRoot = m.mProjectRoot;
      mPatternFile = m.mPatternFile;
      mCacheDir = m.mCacheDir;
      mCacheFilename = m.mCacheFilename;
      mCachePathExpr = m.mCachePathExpr;
      mSkipPatternSearch = m.mSkipPatternSearch;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public PrepConfig build() {
      PrepConfig r = new PrepConfig();
      r.mSave = mSave;
      r.mRestore = mRestore;
      r.mProjectRoot = mProjectRoot;
      r.mPatternFile = mPatternFile;
      r.mCacheDir = mCacheDir;
      r.mCacheFilename = mCacheFilename;
      r.mCachePathExpr = mCachePathExpr;
      r.mSkipPatternSearch = mSkipPatternSearch;
      return r;
    }

    public Builder save(boolean x) {
      mSave = x;
      return this;
    }

    public Builder restore(boolean x) {
      mRestore = x;
      return this;
    }

    public Builder projectRoot(File x) {
      mProjectRoot = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder patternFile(File x) {
      mPatternFile = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder cacheDir(File x) {
      mCacheDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder cacheFilename(File x) {
      mCacheFilename = (x == null) ? _D5 : x;
      return this;
    }

    public Builder cachePathExpr(String x) {
      mCachePathExpr = (x == null) ? "" : x;
      return this;
    }

    public Builder skipPatternSearch(boolean x) {
      mSkipPatternSearch = x;
      return this;
    }

  }

  private static final File _D5 = new File(".prep_oper_cache");

  public static final PrepConfig DEFAULT_INSTANCE = new PrepConfig();

  private PrepConfig() {
    mProjectRoot = Files.DEFAULT;
    mPatternFile = Files.DEFAULT;
    mCacheDir = Files.DEFAULT;
    mCacheFilename = _D5;
    mCachePathExpr = "";
  }

}
