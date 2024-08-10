package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class GetRepoConfig implements AbstractData {

  public String name() {
    return mName;
  }

  public String hash() {
    return mHash;
  }

  public String version() {
    return mVersion;
  }

  public boolean eclipse() {
    return mEclipse;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";
  protected static final String _1 = "hash";
  protected static final String _2 = "version";
  protected static final String _3 = "eclipse";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    m.putUnsafe(_1, mHash);
    m.putUnsafe(_2, mVersion);
    m.putUnsafe(_3, mEclipse);
    return m;
  }

  @Override
  public GetRepoConfig build() {
    return this;
  }

  @Override
  public GetRepoConfig parse(Object obj) {
    return new GetRepoConfig((JSMap) obj);
  }

  private GetRepoConfig(JSMap m) {
    mName = m.opt(_0, "");
    mHash = m.opt(_1, "");
    mVersion = m.opt(_2, "");
    mEclipse = m.opt(_3, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof GetRepoConfig))
      return false;
    GetRepoConfig other = (GetRepoConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mHash.equals(other.mHash)))
      return false;
    if (!(mVersion.equals(other.mVersion)))
      return false;
    if (!(mEclipse == other.mEclipse))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      r = r * 37 + mHash.hashCode();
      r = r * 37 + mVersion.hashCode();
      r = r * 37 + (mEclipse ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected String mHash;
  protected String mVersion;
  protected boolean mEclipse;
  protected int m__hashcode;

  public static final class Builder extends GetRepoConfig {

    private Builder(GetRepoConfig m) {
      mName = m.mName;
      mHash = m.mHash;
      mVersion = m.mVersion;
      mEclipse = m.mEclipse;
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
    public GetRepoConfig build() {
      GetRepoConfig r = new GetRepoConfig();
      r.mName = mName;
      r.mHash = mHash;
      r.mVersion = mVersion;
      r.mEclipse = mEclipse;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder hash(String x) {
      mHash = (x == null) ? "" : x;
      return this;
    }

    public Builder version(String x) {
      mVersion = (x == null) ? "" : x;
      return this;
    }

    public Builder eclipse(boolean x) {
      mEclipse = x;
      return this;
    }

  }

  public static final GetRepoConfig DEFAULT_INSTANCE = new GetRepoConfig();

  private GetRepoConfig() {
    mName = "";
    mHash = "";
    mVersion = "";
  }

}
