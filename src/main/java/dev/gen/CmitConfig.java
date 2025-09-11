package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class CmitConfig implements AbstractData {

  public boolean messageOnly() {
    return mMessageOnly;
  }

  public boolean untracked() {
    return mUntracked;
  }

  public boolean issueNumbers() {
    return mIssueNumbers;
  }

  public boolean ignoreConflict() {
    return mIgnoreConflict;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "message_only";
  protected static final String _1 = "untracked";
  protected static final String _2 = "issue_numbers";
  protected static final String _3 = "ignore_conflict";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mMessageOnly);
    m.putUnsafe(_1, mUntracked);
    m.putUnsafe(_2, mIssueNumbers);
    m.putUnsafe(_3, mIgnoreConflict);
    return m;
  }

  @Override
  public CmitConfig build() {
    return this;
  }

  @Override
  public CmitConfig parse(Object obj) {
    return new CmitConfig((JSMap) obj);
  }

  private CmitConfig(JSMap m) {
    mMessageOnly = m.opt(_0, false);
    mUntracked = m.opt(_1, false);
    mIssueNumbers = m.opt(_2, false);
    mIgnoreConflict = m.opt(_3, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CmitConfig))
      return false;
    CmitConfig other = (CmitConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mMessageOnly == other.mMessageOnly))
      return false;
    if (!(mUntracked == other.mUntracked))
      return false;
    if (!(mIssueNumbers == other.mIssueNumbers))
      return false;
    if (!(mIgnoreConflict == other.mIgnoreConflict))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + (mMessageOnly ? 1 : 0);
      r = r * 37 + (mUntracked ? 1 : 0);
      r = r * 37 + (mIssueNumbers ? 1 : 0);
      r = r * 37 + (mIgnoreConflict ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected boolean mMessageOnly;
  protected boolean mUntracked;
  protected boolean mIssueNumbers;
  protected boolean mIgnoreConflict;
  protected int m__hashcode;

  public static final class Builder extends CmitConfig {

    private Builder(CmitConfig m) {
      mMessageOnly = m.mMessageOnly;
      mUntracked = m.mUntracked;
      mIssueNumbers = m.mIssueNumbers;
      mIgnoreConflict = m.mIgnoreConflict;
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
    public CmitConfig build() {
      CmitConfig r = new CmitConfig();
      r.mMessageOnly = mMessageOnly;
      r.mUntracked = mUntracked;
      r.mIssueNumbers = mIssueNumbers;
      r.mIgnoreConflict = mIgnoreConflict;
      return r;
    }

    public Builder messageOnly(boolean x) {
      mMessageOnly = x;
      return this;
    }

    public Builder untracked(boolean x) {
      mUntracked = x;
      return this;
    }

    public Builder issueNumbers(boolean x) {
      mIssueNumbers = x;
      return this;
    }

    public Builder ignoreConflict(boolean x) {
      mIgnoreConflict = x;
      return this;
    }

  }

  public static final CmitConfig DEFAULT_INSTANCE = new CmitConfig();

  private CmitConfig() {
  }

}
