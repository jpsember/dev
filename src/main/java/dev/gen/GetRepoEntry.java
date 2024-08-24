package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class GetRepoEntry implements AbstractData {

  public String repoName() {
    return mRepoName;
  }

  public String commitHash() {
    return mCommitHash;
  }

  public String version() {
    return mVersion;
  }

  public String groupId() {
    return mGroupId;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "repo_name";
  protected static final String _1 = "commit_hash";
  protected static final String _2 = "version";
  protected static final String _3 = "group_id";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mRepoName);
    m.putUnsafe(_1, mCommitHash);
    m.putUnsafe(_2, mVersion);
    m.putUnsafe(_3, mGroupId);
    return m;
  }

  @Override
  public GetRepoEntry build() {
    return this;
  }

  @Override
  public GetRepoEntry parse(Object obj) {
    return new GetRepoEntry((JSMap) obj);
  }

  private GetRepoEntry(JSMap m) {
    mRepoName = m.opt(_0, "");
    mCommitHash = m.opt(_1, "");
    mVersion = m.opt(_2, "");
    mGroupId = m.opt(_3, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof GetRepoEntry))
      return false;
    GetRepoEntry other = (GetRepoEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mRepoName.equals(other.mRepoName)))
      return false;
    if (!(mCommitHash.equals(other.mCommitHash)))
      return false;
    if (!(mVersion.equals(other.mVersion)))
      return false;
    if (!(mGroupId.equals(other.mGroupId)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mRepoName.hashCode();
      r = r * 37 + mCommitHash.hashCode();
      r = r * 37 + mVersion.hashCode();
      r = r * 37 + mGroupId.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mRepoName;
  protected String mCommitHash;
  protected String mVersion;
  protected String mGroupId;
  protected int m__hashcode;

  public static final class Builder extends GetRepoEntry {

    private Builder(GetRepoEntry m) {
      mRepoName = m.mRepoName;
      mCommitHash = m.mCommitHash;
      mVersion = m.mVersion;
      mGroupId = m.mGroupId;
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
    public GetRepoEntry build() {
      GetRepoEntry r = new GetRepoEntry();
      r.mRepoName = mRepoName;
      r.mCommitHash = mCommitHash;
      r.mVersion = mVersion;
      r.mGroupId = mGroupId;
      return r;
    }

    public Builder repoName(String x) {
      mRepoName = (x == null) ? "" : x;
      return this;
    }

    public Builder commitHash(String x) {
      mCommitHash = (x == null) ? "" : x;
      return this;
    }

    public Builder version(String x) {
      mVersion = (x == null) ? "" : x;
      return this;
    }

    public Builder groupId(String x) {
      mGroupId = (x == null) ? "" : x;
      return this;
    }

  }

  public static final GetRepoEntry DEFAULT_INSTANCE = new GetRepoEntry();

  private GetRepoEntry() {
    mRepoName = "";
    mCommitHash = "";
    mVersion = "";
    mGroupId = "";
  }

}
