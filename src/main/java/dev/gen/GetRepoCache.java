package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class GetRepoCache implements AbstractData {

  public int version() {
    return mVersion;
  }

  public JSMap repoMap() {
    return mRepoMap;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "repo_map";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersion);
    m.putUnsafe(_1, mRepoMap);
    return m;
  }

  @Override
  public GetRepoCache build() {
    return this;
  }

  @Override
  public GetRepoCache parse(Object obj) {
    return new GetRepoCache((JSMap) obj);
  }

  private GetRepoCache(JSMap m) {
    mVersion = m.opt(_0, 1);
    {
      mRepoMap = JSMap.DEFAULT_INSTANCE;
      JSMap x = m.optJSMap(_1);
      if (x != null) {
        mRepoMap = x.lock();
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
    if (object == null || !(object instanceof GetRepoCache))
      return false;
    GetRepoCache other = (GetRepoCache) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersion == other.mVersion))
      return false;
    if (!(mRepoMap.equals(other.mRepoMap)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion;
      r = r * 37 + mRepoMap.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mVersion;
  protected JSMap mRepoMap;
  protected int m__hashcode;

  public static final class Builder extends GetRepoCache {

    private Builder(GetRepoCache m) {
      mVersion = m.mVersion;
      mRepoMap = m.mRepoMap;
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
    public GetRepoCache build() {
      GetRepoCache r = new GetRepoCache();
      r.mVersion = mVersion;
      r.mRepoMap = mRepoMap;
      return r;
    }

    public Builder version(int x) {
      mVersion = x;
      return this;
    }

    public Builder repoMap(JSMap x) {
      mRepoMap = (x == null) ? JSMap.DEFAULT_INSTANCE : x;
      return this;
    }

  }

  public static final GetRepoCache DEFAULT_INSTANCE = new GetRepoCache();

  private GetRepoCache() {
    mVersion = 1;
    mRepoMap = JSMap.DEFAULT_INSTANCE;
  }

}
