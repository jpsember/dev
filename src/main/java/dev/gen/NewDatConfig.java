package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class NewDatConfig implements AbstractData {

  public String name() {
    return mName;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    return m;
  }

  @Override
  public NewDatConfig build() {
    return this;
  }

  @Override
  public NewDatConfig parse(Object obj) {
    return new NewDatConfig((JSMap) obj);
  }

  private NewDatConfig(JSMap m) {
    mName = m.opt(_0, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof NewDatConfig))
      return false;
    NewDatConfig other = (NewDatConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected int m__hashcode;

  public static final class Builder extends NewDatConfig {

    private Builder(NewDatConfig m) {
      mName = m.mName;
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
    public NewDatConfig build() {
      NewDatConfig r = new NewDatConfig();
      r.mName = mName;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

  }

  public static final NewDatConfig DEFAULT_INSTANCE = new NewDatConfig();

  private NewDatConfig() {
    mName = "";
  }

}
