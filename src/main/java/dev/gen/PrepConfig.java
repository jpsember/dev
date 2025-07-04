package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class PrepConfig implements AbstractData {

  public File dir() {
    return mDir;
  }

  public boolean save() {
    return mSave;
  }

  public boolean restore() {
    return mRestore;
  }

  public File projectFile() {
    return mProjectFile;
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

  protected static final String _0 = "dir";
  protected static final String _1 = "save";
  protected static final String _2 = "restore";
  protected static final String _3 = "project_file";
  protected static final String _4 = "pattern_file";
  protected static final String _5 = "cache_dir";
  protected static final String _6 = "cache_filename";
  protected static final String _7 = "cache_path_expr";
  protected static final String _8 = "skip_pattern_search";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mDir.toString());
    m.putUnsafe(_1, mSave);
    m.putUnsafe(_2, mRestore);
    m.putUnsafe(_3, mProjectFile.toString());
    m.putUnsafe(_4, mPatternFile.toString());
    m.putUnsafe(_5, mCacheDir.toString());
    m.putUnsafe(_6, mCacheFilename.toString());
    m.putUnsafe(_7, mCachePathExpr);
    m.putUnsafe(_8, mSkipPatternSearch);
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
    {
      mDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mDir = new File(x);
      }
    }
    mSave = m.opt(_1, false);
    mRestore = m.opt(_2, false);
    {
      mProjectFile = _D3;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mProjectFile = new File(x);
      }
    }
    {
      mPatternFile = Files.DEFAULT;
      String x = m.opt(_4, (String) null);
      if (x != null) {
        mPatternFile = new File(x);
      }
    }
    {
      mCacheDir = Files.DEFAULT;
      String x = m.opt(_5, (String) null);
      if (x != null) {
        mCacheDir = new File(x);
      }
    }
    {
      mCacheFilename = _D6;
      String x = m.opt(_6, (String) null);
      if (x != null) {
        mCacheFilename = new File(x);
      }
    }
    mCachePathExpr = m.opt(_7, "");
    mSkipPatternSearch = m.opt(_8, false);
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
    if (!(mDir.equals(other.mDir)))
      return false;
    if (!(mSave == other.mSave))
      return false;
    if (!(mRestore == other.mRestore))
      return false;
    if (!(mProjectFile.equals(other.mProjectFile)))
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
      r = r * 37 + mDir.hashCode();
      r = r * 37 + (mSave ? 1 : 0);
      r = r * 37 + (mRestore ? 1 : 0);
      r = r * 37 + mProjectFile.hashCode();
      r = r * 37 + mPatternFile.hashCode();
      r = r * 37 + mCacheDir.hashCode();
      r = r * 37 + mCacheFilename.hashCode();
      r = r * 37 + mCachePathExpr.hashCode();
      r = r * 37 + (mSkipPatternSearch ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mDir;
  protected boolean mSave;
  protected boolean mRestore;
  protected File mProjectFile;
  protected File mPatternFile;
  protected File mCacheDir;
  protected File mCacheFilename;
  protected String mCachePathExpr;
  protected boolean mSkipPatternSearch;
  protected int m__hashcode;

  public static final class Builder extends PrepConfig {

    private Builder(PrepConfig m) {
      mDir = m.mDir;
      mSave = m.mSave;
      mRestore = m.mRestore;
      mProjectFile = m.mProjectFile;
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
      r.mDir = mDir;
      r.mSave = mSave;
      r.mRestore = mRestore;
      r.mProjectFile = mProjectFile;
      r.mPatternFile = mPatternFile;
      r.mCacheDir = mCacheDir;
      r.mCacheFilename = mCacheFilename;
      r.mCachePathExpr = mCachePathExpr;
      r.mSkipPatternSearch = mSkipPatternSearch;
      return r;
    }

    public Builder dir(File x) {
      mDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder save(boolean x) {
      mSave = x;
      return this;
    }

    public Builder restore(boolean x) {
      mRestore = x;
      return this;
    }

    public Builder projectFile(File x) {
      mProjectFile = (x == null) ? _D3 : x;
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
      mCacheFilename = (x == null) ? _D6 : x;
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

  private static final File _D3 = new File(".git");
  private static final File _D6 = new File(".prep_oper_cache");

  public static final PrepConfig DEFAULT_INSTANCE = new PrepConfig();

  private PrepConfig() {
    mDir = Files.DEFAULT;
    mProjectFile = _D3;
    mPatternFile = Files.DEFAULT;
    mCacheDir = Files.DEFAULT;
    mCacheFilename = _D6;
    mCachePathExpr = "";
  }

}
