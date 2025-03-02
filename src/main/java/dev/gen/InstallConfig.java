package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class InstallConfig implements AbstractData {

  public String program() {
    return mProgram;
  }

  public String repo() {
    return mRepo;
  }

  public String mainClass() {
    return mMainClass;
  }

  public boolean skipTests() {
    return mSkipTests;
  }

  public String branch() {
    return mBranch;
  }

  public boolean force() {
    return mForce;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "program";
  protected static final String _1 = "repo";
  protected static final String _2 = "main_class";
  protected static final String _3 = "skip_tests";
  protected static final String _4 = "branch";
  protected static final String _5 = "force";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mProgram);
    m.putUnsafe(_1, mRepo);
    m.putUnsafe(_2, mMainClass);
    m.putUnsafe(_3, mSkipTests);
    m.putUnsafe(_4, mBranch);
    m.putUnsafe(_5, mForce);
    return m;
  }

  @Override
  public InstallConfig build() {
    return this;
  }

  @Override
  public InstallConfig parse(Object obj) {
    return new InstallConfig((JSMap) obj);
  }

  private InstallConfig(JSMap m) {
    mProgram = m.opt(_0, "");
    mRepo = m.opt(_1, "");
    mMainClass = m.opt(_2, "");
    mSkipTests = m.opt(_3, false);
    mBranch = m.opt(_4, "");
    mForce = m.opt(_5, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof InstallConfig))
      return false;
    InstallConfig other = (InstallConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mProgram.equals(other.mProgram)))
      return false;
    if (!(mRepo.equals(other.mRepo)))
      return false;
    if (!(mMainClass.equals(other.mMainClass)))
      return false;
    if (!(mSkipTests == other.mSkipTests))
      return false;
    if (!(mBranch.equals(other.mBranch)))
      return false;
    if (!(mForce == other.mForce))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mProgram.hashCode();
      r = r * 37 + mRepo.hashCode();
      r = r * 37 + mMainClass.hashCode();
      r = r * 37 + (mSkipTests ? 1 : 0);
      r = r * 37 + mBranch.hashCode();
      r = r * 37 + (mForce ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected String mProgram;
  protected String mRepo;
  protected String mMainClass;
  protected boolean mSkipTests;
  protected String mBranch;
  protected boolean mForce;
  protected int m__hashcode;

  public static final class Builder extends InstallConfig {

    private Builder(InstallConfig m) {
      mProgram = m.mProgram;
      mRepo = m.mRepo;
      mMainClass = m.mMainClass;
      mSkipTests = m.mSkipTests;
      mBranch = m.mBranch;
      mForce = m.mForce;
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
    public InstallConfig build() {
      InstallConfig r = new InstallConfig();
      r.mProgram = mProgram;
      r.mRepo = mRepo;
      r.mMainClass = mMainClass;
      r.mSkipTests = mSkipTests;
      r.mBranch = mBranch;
      r.mForce = mForce;
      return r;
    }

    public Builder program(String x) {
      mProgram = (x == null) ? "" : x;
      return this;
    }

    public Builder repo(String x) {
      mRepo = (x == null) ? "" : x;
      return this;
    }

    public Builder mainClass(String x) {
      mMainClass = (x == null) ? "" : x;
      return this;
    }

    public Builder skipTests(boolean x) {
      mSkipTests = x;
      return this;
    }

    public Builder branch(String x) {
      mBranch = (x == null) ? "" : x;
      return this;
    }

    public Builder force(boolean x) {
      mForce = x;
      return this;
    }

  }

  public static final InstallConfig DEFAULT_INSTANCE = new InstallConfig();

  private InstallConfig() {
    mProgram = "";
    mRepo = "";
    mMainClass = "";
    mBranch = "";
  }

}
