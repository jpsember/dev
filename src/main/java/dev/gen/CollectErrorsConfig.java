package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class CollectErrorsConfig implements AbstractData {

  public File input() {
    return mInput;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "input";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mInput.toString());
    return m;
  }

  @Override
  public CollectErrorsConfig build() {
    return this;
  }

  @Override
  public CollectErrorsConfig parse(Object obj) {
    return new CollectErrorsConfig((JSMap) obj);
  }

  private CollectErrorsConfig(JSMap m) {
    {
      mInput = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mInput = new File(x);
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
    if (object == null || !(object instanceof CollectErrorsConfig))
      return false;
    CollectErrorsConfig other = (CollectErrorsConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mInput.equals(other.mInput)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mInput.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected File mInput;
  protected int m__hashcode;

  public static final class Builder extends CollectErrorsConfig {

    private Builder(CollectErrorsConfig m) {
      mInput = m.mInput;
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
    public CollectErrorsConfig build() {
      CollectErrorsConfig r = new CollectErrorsConfig();
      r.mInput = mInput;
      return r;
    }

    public Builder input(File x) {
      mInput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

  }

  public static final CollectErrorsConfig DEFAULT_INSTANCE = new CollectErrorsConfig();

  private CollectErrorsConfig() {
    mInput = Files.DEFAULT;
  }

}
