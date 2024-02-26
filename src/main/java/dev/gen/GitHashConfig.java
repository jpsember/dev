package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.json.JSMap;

public class GitHashConfig implements AbstractData {

  public File source() {
    return mSource;
  }

  public String pattern() {
    return mPattern;
  }

  public File repoDirs() {
    return mRepoDirs;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "source";
  protected static final String _1 = "pattern";
  protected static final String _2 = "repo_dirs";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mSource.toString());
    m.putUnsafe(_1, mPattern);
    m.putUnsafe(_2, mRepoDirs.toString());
    return m;
  }

  @Override
  public GitHashConfig build() {
    return this;
  }

  @Override
  public GitHashConfig parse(Object obj) {
    return new GitHashConfig((JSMap) obj);
  }

  private GitHashConfig(JSMap m) {
    {
      mSource = _D0;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mSource = new File(x);
      }
    }
    mPattern = m.opt(_1, "pull_dep\\s+(\\w+)\\s+([a-gA-G0-9]+)");
    {
      mRepoDirs = _D2;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mRepoDirs = new File(x);
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
    if (object == null || !(object instanceof GitHashConfig))
      return false;
    GitHashConfig other = (GitHashConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSource.equals(other.mSource)))
      return false;
    if (!(mPattern.equals(other.mPattern)))
      return false;
    if (!(mRepoDirs.equals(other.mRepoDirs)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mSource.hashCode();
      r = r * 37 + mPattern.hashCode();
      r = r * 37 + mRepoDirs.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected File mSource;
  protected String mPattern;
  protected File mRepoDirs;
  protected int m__hashcode;

  public static final class Builder extends GitHashConfig {

    private Builder(GitHashConfig m) {
      mSource = m.mSource;
      mPattern = m.mPattern;
      mRepoDirs = m.mRepoDirs;
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
    public GitHashConfig build() {
      GitHashConfig r = new GitHashConfig();
      r.mSource = mSource;
      r.mPattern = mPattern;
      r.mRepoDirs = mRepoDirs;
      return r;
    }

    public Builder source(File x) {
      mSource = (x == null) ? _D0 : x;
      return this;
    }

    public Builder pattern(String x) {
      mPattern = (x == null) ? "pull_dep\\s+(\\w+)\\s+([a-gA-G0-9]+)" : x;
      return this;
    }

    public Builder repoDirs(File x) {
      mRepoDirs = (x == null) ? _D2 : x;
      return this;
    }

  }

  private static final File _D0 = new File("pull_deps.sh");
  private static final File _D2 = new File("/Users/home/github_projects");

  public static final GitHashConfig DEFAULT_INSTANCE = new GitHashConfig();

  private GitHashConfig() {
    mSource = _D0;
    mPattern = "pull_dep\\s+(\\w+)\\s+([a-gA-G0-9]+)";
    mRepoDirs = _D2;
  }

}
