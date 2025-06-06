package dev.gen.archive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSMap;

public class LocalRegistry implements AbstractData {

  public String version() {
    return mVersion;
  }

  public Map<String, LocalEntry> entries() {
    return mEntries;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "entries";

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
      for (Map.Entry<String, LocalEntry> e : mEntries.entrySet())
        j.put(e.getKey(), e.getValue().toJson());
      m.put(_1, j);
    }
    return m;
  }

  @Override
  public LocalRegistry build() {
    return this;
  }

  @Override
  public LocalRegistry parse(Object obj) {
    return new LocalRegistry((JSMap) obj);
  }

  private LocalRegistry(JSMap m) {
    mVersion = m.opt(_0, "2.0");
    {
      mEntries = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("entries");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, LocalEntry> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), LocalEntry.DEFAULT_INSTANCE.parse((JSMap) e.getValue()));
          mEntries = mp;
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
    if (object == null || !(object instanceof LocalRegistry))
      return false;
    LocalRegistry other = (LocalRegistry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersion.equals(other.mVersion)))
      return false;
    if (!(mEntries.equals(other.mEntries)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion.hashCode();
      r = r * 37 + mEntries.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mVersion;
  protected Map<String, LocalEntry> mEntries;
  protected int m__hashcode;

  public static final class Builder extends LocalRegistry {

    private Builder(LocalRegistry m) {
      mVersion = m.mVersion;
      mEntries = m.mEntries;
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
    public LocalRegistry build() {
      LocalRegistry r = new LocalRegistry();
      r.mVersion = mVersion;
      r.mEntries = mEntries;
      return r;
    }

    public Builder version(String x) {
      mVersion = (x == null) ? "2.0" : x;
      return this;
    }

    public Builder entries(Map<String, LocalEntry> x) {
      mEntries = (x == null) ? DataUtil.emptyMap() : x;
      return this;
    }

  }

  public static final LocalRegistry DEFAULT_INSTANCE = new LocalRegistry();

  private LocalRegistry() {
    mVersion = "2.0";
    mEntries = DataUtil.emptyMap();
  }

}
