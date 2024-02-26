package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class RemoteConfig implements AbstractData {

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    return m;
  }

  @Override
  public RemoteConfig build() {
    return this;
  }

  @Override
  public RemoteConfig parse(Object obj) {
    return new RemoteConfig((JSMap) obj);
  }

  private RemoteConfig(JSMap m) {
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof RemoteConfig))
      return false;
    RemoteConfig other = (RemoteConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      m__hashcode = r;
    }
    return r;
  }

  protected int m__hashcode;

  public static final class Builder extends RemoteConfig {

    private Builder(RemoteConfig m) {

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
    public RemoteConfig build() {
      RemoteConfig r = new RemoteConfig();

      return r;
    }

  }

  public static final RemoteConfig DEFAULT_INSTANCE = new RemoteConfig();

  private RemoteConfig() {
  }

}
