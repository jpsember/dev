package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class FetchCloudConfig implements AbstractData {

  public int maxFetchCount() {
    return mMaxFetchCount;
  }

  public String subfolderPath() {
    return mSubfolderPath;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "max_fetch_count";
  protected static final String _1 = "subfolder_path";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mMaxFetchCount);
    m.putUnsafe(_1, mSubfolderPath);
    return m;
  }

  @Override
  public FetchCloudConfig build() {
    return this;
  }

  @Override
  public FetchCloudConfig parse(Object obj) {
    return new FetchCloudConfig((JSMap) obj);
  }

  private FetchCloudConfig(JSMap m) {
    mMaxFetchCount = m.opt(_0, 5);
    mSubfolderPath = m.opt(_1, "cloud_writer");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof FetchCloudConfig))
      return false;
    FetchCloudConfig other = (FetchCloudConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mMaxFetchCount == other.mMaxFetchCount))
      return false;
    if (!(mSubfolderPath.equals(other.mSubfolderPath)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mMaxFetchCount;
      r = r * 37 + mSubfolderPath.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mMaxFetchCount;
  protected String mSubfolderPath;
  protected int m__hashcode;

  public static final class Builder extends FetchCloudConfig {

    private Builder(FetchCloudConfig m) {
      mMaxFetchCount = m.mMaxFetchCount;
      mSubfolderPath = m.mSubfolderPath;
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
    public FetchCloudConfig build() {
      FetchCloudConfig r = new FetchCloudConfig();
      r.mMaxFetchCount = mMaxFetchCount;
      r.mSubfolderPath = mSubfolderPath;
      return r;
    }

    public Builder maxFetchCount(int x) {
      mMaxFetchCount = x;
      return this;
    }

    public Builder subfolderPath(String x) {
      mSubfolderPath = (x == null) ? "cloud_writer" : x;
      return this;
    }

  }

  public static final FetchCloudConfig DEFAULT_INSTANCE = new FetchCloudConfig();

  private FetchCloudConfig() {
    mMaxFetchCount = 5;
    mSubfolderPath = "cloud_writer";
  }

}
