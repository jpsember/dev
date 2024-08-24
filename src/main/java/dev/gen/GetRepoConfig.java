package dev.gen;

import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSList;
import js.json.JSMap;

public class GetRepoConfig implements AbstractData {

  public List<GetRepoEntry> entries() {
    return mEntries;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "entries";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    {
      JSList j = new JSList();
      for (GetRepoEntry x : mEntries)
        j.add(x.toJson());
      m.put(_0, j);
    }
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
    mEntries = DataUtil.parseListOfObjects(GetRepoEntry.DEFAULT_INSTANCE, m.optJSList(_0), false);
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
    if (!(mEntries.equals(other.mEntries)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      for (GetRepoEntry x : mEntries)
        if (x != null)
          r = r * 37 + x.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected List<GetRepoEntry> mEntries;
  protected int m__hashcode;

  public static final class Builder extends GetRepoConfig {

    private Builder(GetRepoConfig m) {
      mEntries = DataUtil.immutableCopyOf(m.mEntries) /*DEBUG*/ ;
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
      r.mEntries = mEntries;
      return r;
    }

    public Builder entries(List<GetRepoEntry> x) {
      mEntries = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final GetRepoConfig DEFAULT_INSTANCE = new GetRepoConfig();

  private GetRepoConfig() {
    mEntries = DataUtil.emptyList();
  }

}
