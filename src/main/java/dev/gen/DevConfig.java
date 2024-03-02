package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class DevConfig implements AbstractData {

  public String notUsedYet() {
    return mNotUsedYet;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "not_used_yet";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mNotUsedYet);
    return m;
  }

  @Override
  public DevConfig build() {
    return this;
  }

  @Override
  public DevConfig parse(Object obj) {
    return new DevConfig((JSMap) obj);
  }

  private DevConfig(JSMap m) {
    mNotUsedYet = m.opt(_0, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof DevConfig))
      return false;
    DevConfig other = (DevConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mNotUsedYet.equals(other.mNotUsedYet)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mNotUsedYet.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mNotUsedYet;
  protected int m__hashcode;

  public static final class Builder extends DevConfig {

    private Builder(DevConfig m) {
      mNotUsedYet = m.mNotUsedYet;
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
    public DevConfig build() {
      DevConfig r = new DevConfig();
      r.mNotUsedYet = mNotUsedYet;
      return r;
    }

    public Builder notUsedYet(String x) {
      mNotUsedYet = (x == null) ? "" : x;
      return this;
    }

  }

  public static final DevConfig DEFAULT_INSTANCE = new DevConfig();

  private DevConfig() {
    mNotUsedYet = "";
  }

}
