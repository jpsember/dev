package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class StripConfig implements AbstractData {

  public String sourceBranch() {
    return mSourceBranch;
  }

  public String targetBranch() {
    return mTargetBranch;
  }

  public boolean defaults() {
    return mDefaults;
  }

  public File projectDir() {
    return mProjectDir;
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

  protected static final String _0 = "source_branch";
  protected static final String _1 = "target_branch";
  protected static final String _2 = "defaults";
  protected static final String _3 = "project_dir";
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
    m.putUnsafe(_0, mSourceBranch);
    m.putUnsafe(_1, mTargetBranch);
    m.putUnsafe(_2, mDefaults);
    m.putUnsafe(_3, mProjectDir.toString());
    m.putUnsafe(_4, mCacheDir.toString());
    m.putUnsafe(_5, mCacheFilename.toString());
    m.putUnsafe(_6, mCachePathExpr);
    m.putUnsafe(_7, mSkipPatternSearch);
    return m;
  }

  @Override
  public StripConfig build() {
    return this;
  }

  @Override
  public StripConfig parse(Object obj) {
    return new StripConfig((JSMap) obj);
  }

  private StripConfig(JSMap m) {
    mSourceBranch = m.opt(_0, "");
    mTargetBranch = m.opt(_1, "");
    mDefaults = m.opt(_2, false);
    {
      mProjectDir = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mProjectDir = new File(x);
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
    if (object == null || !(object instanceof StripConfig))
      return false;
    StripConfig other = (StripConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSourceBranch.equals(other.mSourceBranch)))
      return false;
    if (!(mTargetBranch.equals(other.mTargetBranch)))
      return false;
    if (!(mDefaults == other.mDefaults))
      return false;
    if (!(mProjectDir.equals(other.mProjectDir)))
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
      r = r * 37 + mSourceBranch.hashCode();
      r = r * 37 + mTargetBranch.hashCode();
      r = r * 37 + (mDefaults ? 1 : 0);
      r = r * 37 + mProjectDir.hashCode();
      r = r * 37 + mCacheDir.hashCode();
      r = r * 37 + mCacheFilename.hashCode();
      r = r * 37 + mCachePathExpr.hashCode();
      r = r * 37 + (mSkipPatternSearch ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected String mSourceBranch;
  protected String mTargetBranch;
  protected boolean mDefaults;
  protected File mProjectDir;
  protected File mCacheDir;
  protected File mCacheFilename;
  protected String mCachePathExpr;
  protected boolean mSkipPatternSearch;
  protected int m__hashcode;

  public static final class Builder extends StripConfig {

    private Builder(StripConfig m) {
      mSourceBranch = m.mSourceBranch;
      mTargetBranch = m.mTargetBranch;
      mDefaults = m.mDefaults;
      mProjectDir = m.mProjectDir;
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
    public StripConfig build() {
      StripConfig r = new StripConfig();
      r.mSourceBranch = mSourceBranch;
      r.mTargetBranch = mTargetBranch;
      r.mDefaults = mDefaults;
      r.mProjectDir = mProjectDir;
      r.mCacheDir = mCacheDir;
      r.mCacheFilename = mCacheFilename;
      r.mCachePathExpr = mCachePathExpr;
      r.mSkipPatternSearch = mSkipPatternSearch;
      return r;
    }

    public Builder sourceBranch(String x) {
      mSourceBranch = (x == null) ? "" : x;
      return this;
    }

    public Builder targetBranch(String x) {
      mTargetBranch = (x == null) ? "" : x;
      return this;
    }

    public Builder defaults(boolean x) {
      mDefaults = x;
      return this;
    }

    public Builder projectDir(File x) {
      mProjectDir = (x == null) ? Files.DEFAULT : x;
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

  private static final File _D5 = new File(".strip_oper_cache");

  public static final StripConfig DEFAULT_INSTANCE = new StripConfig();

  private StripConfig() {
    mSourceBranch = "";
    mTargetBranch = "";
    mProjectDir = Files.DEFAULT;
    mCacheDir = Files.DEFAULT;
    mCacheFilename = _D5;
    mCachePathExpr = "";
  }

}
