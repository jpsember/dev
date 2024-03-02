package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class CreateAppConfig implements AbstractData {

  public String name() {
    return mName;
  }

  public String mainPackage() {
    return mMainPackage;
  }

  public String mainClassName() {
    return mMainClassName;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";
  protected static final String _1 = "main_package";
  protected static final String _2 = "main_class_name";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    m.putUnsafe(_1, mMainPackage);
    m.putUnsafe(_2, mMainClassName);
    return m;
  }

  @Override
  public CreateAppConfig build() {
    return this;
  }

  @Override
  public CreateAppConfig parse(Object obj) {
    return new CreateAppConfig((JSMap) obj);
  }

  private CreateAppConfig(JSMap m) {
    mName = m.opt(_0, "");
    mMainPackage = m.opt(_1, "");
    mMainClassName = m.opt(_2, "Main");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CreateAppConfig))
      return false;
    CreateAppConfig other = (CreateAppConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mMainPackage.equals(other.mMainPackage)))
      return false;
    if (!(mMainClassName.equals(other.mMainClassName)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      r = r * 37 + mMainPackage.hashCode();
      r = r * 37 + mMainClassName.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected String mMainPackage;
  protected String mMainClassName;
  protected int m__hashcode;

  public static final class Builder extends CreateAppConfig {

    private Builder(CreateAppConfig m) {
      mName = m.mName;
      mMainPackage = m.mMainPackage;
      mMainClassName = m.mMainClassName;
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
    public CreateAppConfig build() {
      CreateAppConfig r = new CreateAppConfig();
      r.mName = mName;
      r.mMainPackage = mMainPackage;
      r.mMainClassName = mMainClassName;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder mainPackage(String x) {
      mMainPackage = (x == null) ? "" : x;
      return this;
    }

    public Builder mainClassName(String x) {
      mMainClassName = (x == null) ? "Main" : x;
      return this;
    }

  }

  public static final CreateAppConfig DEFAULT_INSTANCE = new CreateAppConfig();

  private CreateAppConfig() {
    mName = "";
    mMainPackage = "";
    mMainClassName = "Main";
  }

}
