package dev.gen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSMap;

public class RepoInfoRecord implements AbstractData {

  public Map<String, String> commitToVersionMap() {
    return mCommitToVersionMap;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "commit_to_version_map";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, String> e : mCommitToVersionMap.entrySet())
        j.put(e.getKey(), e.getValue());
      m.put(_0, j);
    }
    return m;
  }

  @Override
  public RepoInfoRecord build() {
    return this;
  }

  @Override
  public RepoInfoRecord parse(Object obj) {
    return new RepoInfoRecord((JSMap) obj);
  }

  private RepoInfoRecord(JSMap m) {
    {
      mCommitToVersionMap = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("commit_to_version_map");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, String> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), (String) e.getValue());
          mCommitToVersionMap = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
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
    if (object == null || !(object instanceof RepoInfoRecord))
      return false;
    RepoInfoRecord other = (RepoInfoRecord) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mCommitToVersionMap.equals(other.mCommitToVersionMap)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mCommitToVersionMap.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected Map<String, String> mCommitToVersionMap;
  protected int m__hashcode;

  public static final class Builder extends RepoInfoRecord {

    private Builder(RepoInfoRecord m) {
      mCommitToVersionMap = DataUtil.immutableCopyOf(m.mCommitToVersionMap) /*DEBUG*/ ;
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
    public RepoInfoRecord build() {
      RepoInfoRecord r = new RepoInfoRecord();
      r.mCommitToVersionMap = mCommitToVersionMap;
      return r;
    }

    public Builder commitToVersionMap(Map<String, String> x) {
      mCommitToVersionMap = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final RepoInfoRecord DEFAULT_INSTANCE = new RepoInfoRecord();

  private RepoInfoRecord() {
    mCommitToVersionMap = DataUtil.emptyMap();
  }

}
