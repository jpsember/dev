package dev.gen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSMap;

public class GetRepoCache implements AbstractData {

  public int version() {
    return mVersion;
  }

  public Map<String, RepoInfoRecord> cache() {
    return mCache;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "cache";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersion);
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, RepoInfoRecord> e : mCache.entrySet())
        j.put(e.getKey(), e.getValue().toJson());
      m.put(_1, j);
    }
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
      mCache = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("cache");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, RepoInfoRecord> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), RepoInfoRecord.DEFAULT_INSTANCE.parse((JSMap) e.getValue()));
          mCache = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
        }
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
    if (!(mCache.equals(other.mCache)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion;
      r = r * 37 + mCache.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mVersion;
  protected Map<String, RepoInfoRecord> mCache;
  protected int m__hashcode;

  public static final class Builder extends GetRepoCache {

    private Builder(GetRepoCache m) {
      mVersion = m.mVersion;
      mCache = DataUtil.immutableCopyOf(m.mCache) /*DEBUG*/ ;
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
      r.mCache = mCache;
      return r;
    }

    public Builder version(int x) {
      mVersion = x;
      return this;
    }

    public Builder cache(Map<String, RepoInfoRecord> x) {
      mCache = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final GetRepoCache DEFAULT_INSTANCE = new GetRepoCache();

  private GetRepoCache() {
    mVersion = 1;
    mCache = DataUtil.emptyMap();
  }

}
