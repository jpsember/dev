package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class PrepConfig implements AbstractData {

  public PrepOperType oper() {
    return mOper;
  }

  public String sourceBranch() {
    return mSourceBranch;
  }

  public String targetBranch() {
    return mTargetBranch;
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

  public File projectRootForTesting() {
    return mProjectRootForTesting;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "oper";
  protected static final String _1 = "source_branch";
  protected static final String _2 = "target_branch";
  protected static final String _3 = "cache_dir";
  protected static final String _4 = "cache_filename";
  protected static final String _5 = "cache_path_expr";
  protected static final String _6 = "skip_pattern_search";
  protected static final String _7 = "project_root_for_testing";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mOper.toString().toLowerCase());
    m.putUnsafe(_1, mSourceBranch);
    m.putUnsafe(_2, mTargetBranch);
    m.putUnsafe(_3, mCacheDir.toString());
    m.putUnsafe(_4, mCacheFilename.toString());
    m.putUnsafe(_5, mCachePathExpr);
    m.putUnsafe(_6, mSkipPatternSearch);
    m.putUnsafe(_7, mProjectRootForTesting.toString());
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
      String x = m.opt(_0, "");
      mOper = x.isEmpty() ? PrepOperType.DEFAULT_INSTANCE : PrepOperType.valueOf(x.toUpperCase());
    }
    mSourceBranch = m.opt(_1, "");
    mTargetBranch = m.opt(_2, "");
    {
      mCacheDir = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mCacheDir = new File(x);
      }
    }
    {
      mCacheFilename = _D4;
      String x = m.opt(_4, (String) null);
      if (x != null) {
        mCacheFilename = new File(x);
      }
    }
    mCachePathExpr = m.opt(_5, "");
    mSkipPatternSearch = m.opt(_6, false);
    {
      mProjectRootForTesting = Files.DEFAULT;
      String x = m.opt(_7, (String) null);
      if (x != null) {
        mProjectRootForTesting = new File(x);
      }
    }
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
    if (!(mOper.equals(other.mOper)))
      return false;
    if (!(mSourceBranch.equals(other.mSourceBranch)))
      return false;
    if (!(mTargetBranch.equals(other.mTargetBranch)))
      return false;
    if (!(mCacheDir.equals(other.mCacheDir)))
      return false;
    if (!(mCacheFilename.equals(other.mCacheFilename)))
      return false;
    if (!(mCachePathExpr.equals(other.mCachePathExpr)))
      return false;
    if (!(mSkipPatternSearch == other.mSkipPatternSearch))
      return false;
    if (!(mProjectRootForTesting.equals(other.mProjectRootForTesting)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mOper.ordinal();
      r = r * 37 + mSourceBranch.hashCode();
      r = r * 37 + mTargetBranch.hashCode();
      r = r * 37 + mCacheDir.hashCode();
      r = r * 37 + mCacheFilename.hashCode();
      r = r * 37 + mCachePathExpr.hashCode();
      r = r * 37 + (mSkipPatternSearch ? 1 : 0);
      r = r * 37 + mProjectRootForTesting.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected PrepOperType mOper;
  protected String mSourceBranch;
  protected String mTargetBranch;
  protected File mCacheDir;
  protected File mCacheFilename;
  protected String mCachePathExpr;
  protected boolean mSkipPatternSearch;
  protected File mProjectRootForTesting;
  protected int m__hashcode;

  public static final class Builder extends PrepConfig {

    private Builder(PrepConfig m) {
      mOper = m.mOper;
      mSourceBranch = m.mSourceBranch;
      mTargetBranch = m.mTargetBranch;
      mCacheDir = m.mCacheDir;
      mCacheFilename = m.mCacheFilename;
      mCachePathExpr = m.mCachePathExpr;
      mSkipPatternSearch = m.mSkipPatternSearch;
      mProjectRootForTesting = m.mProjectRootForTesting;
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
      r.mOper = mOper;
      r.mSourceBranch = mSourceBranch;
      r.mTargetBranch = mTargetBranch;
      r.mCacheDir = mCacheDir;
      r.mCacheFilename = mCacheFilename;
      r.mCachePathExpr = mCachePathExpr;
      r.mSkipPatternSearch = mSkipPatternSearch;
      r.mProjectRootForTesting = mProjectRootForTesting;
      return r;
    }

    public Builder oper(PrepOperType x) {
      mOper = (x == null) ? PrepOperType.DEFAULT_INSTANCE : x;
      return this;
    }

    public Builder sourceBranch(String x) {
      mSourceBranch = (x == null) ? "" : x;
      return this;
    }

    public Builder targetBranch(String x) {
      mTargetBranch = (x == null) ? "" : x;
      return this;
    }

    public Builder cacheDir(File x) {
      mCacheDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder cacheFilename(File x) {
      mCacheFilename = (x == null) ? _D4 : x;
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

    public Builder projectRootForTesting(File x) {
      mProjectRootForTesting = (x == null) ? Files.DEFAULT : x;
      return this;
    }

  }

  private static final File _D4 = new File(".prep_oper_cache");

  public static final PrepConfig DEFAULT_INSTANCE = new PrepConfig();

  private PrepConfig() {
    mOper = PrepOperType.DEFAULT_INSTANCE;
    mSourceBranch = "";
    mTargetBranch = "";
    mCacheDir = Files.DEFAULT;
    mCacheFilename = _D4;
    mCachePathExpr = "";
    mProjectRootForTesting = Files.DEFAULT;
  }

}
