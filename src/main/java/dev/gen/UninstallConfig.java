package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class UninstallConfig implements AbstractData {

  public String program() {
    return mProgram;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "program";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mProgram);
    return m;
  }

  @Override
  public UninstallConfig build() {
    return this;
  }

  @Override
  public UninstallConfig parse(Object obj) {
    return new UninstallConfig((JSMap) obj);
  }

  private UninstallConfig(JSMap m) {
    mProgram = m.opt(_0, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof UninstallConfig))
      return false;
    UninstallConfig other = (UninstallConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mProgram.equals(other.mProgram)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mProgram.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mProgram;
  protected int m__hashcode;

  public static final class Builder extends UninstallConfig {

    private Builder(UninstallConfig m) {
      mProgram = m.mProgram;
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
    public UninstallConfig build() {
      UninstallConfig r = new UninstallConfig();
      r.mProgram = mProgram;
      return r;
    }

    public Builder program(String x) {
      mProgram = (x == null) ? "" : x;
      return this;
    }

  }

  public static final UninstallConfig DEFAULT_INSTANCE = new UninstallConfig();

  private UninstallConfig() {
    mProgram = "";
  }

}
