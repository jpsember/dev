package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class DriverConfig implements AbstractData {

  public String name() {
    return mName;
  }

  public String mainClass() {
    return mMainClass;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";
  protected static final String _1 = "main_class";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    m.putUnsafe(_1, mMainClass);
    return m;
  }

  @Override
  public DriverConfig build() {
    return this;
  }

  @Override
  public DriverConfig parse(Object obj) {
    return new DriverConfig((JSMap) obj);
  }

  private DriverConfig(JSMap m) {
    mName = m.opt(_0, "");
    mMainClass = m.opt(_1, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof DriverConfig))
      return false;
    DriverConfig other = (DriverConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mMainClass.equals(other.mMainClass)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      r = r * 37 + mMainClass.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected String mMainClass;
  protected int m__hashcode;

  public static final class Builder extends DriverConfig {

    private Builder(DriverConfig m) {
      mName = m.mName;
      mMainClass = m.mMainClass;
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
    public DriverConfig build() {
      DriverConfig r = new DriverConfig();
      r.mName = mName;
      r.mMainClass = mMainClass;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder mainClass(String x) {
      mMainClass = (x == null) ? "" : x;
      return this;
    }

  }

  public static final DriverConfig DEFAULT_INSTANCE = new DriverConfig();

  private DriverConfig() {
    mName = "";
    mMainClass = "";
  }

}
