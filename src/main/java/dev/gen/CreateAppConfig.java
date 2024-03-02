package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class CreateAppConfig implements AbstractData {

  public File parentDir() {
    return mParentDir;
  }

  public String name() {
    return mName;
  }

  public boolean omitJsonArgs() {
    return mOmitJsonArgs;
  }

  public File zapExisting() {
    return mZapExisting;
  }

  public boolean eclipse() {
    return mEclipse;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "parent_dir";
  protected static final String _1 = "name";
  protected static final String _2 = "omit_json_args";
  protected static final String _3 = "zap_existing";
  protected static final String _4 = "eclipse";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mParentDir.toString());
    m.putUnsafe(_1, mName);
    m.putUnsafe(_2, mOmitJsonArgs);
    m.putUnsafe(_3, mZapExisting.toString());
    m.putUnsafe(_4, mEclipse);
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
    {
      mParentDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mParentDir = new File(x);
      }
    }
    mName = m.opt(_1, "");
    mOmitJsonArgs = m.opt(_2, false);
    {
      mZapExisting = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mZapExisting = new File(x);
      }
    }
    mEclipse = m.opt(_4, false);
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
    if (!(mParentDir.equals(other.mParentDir)))
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mOmitJsonArgs == other.mOmitJsonArgs))
      return false;
    if (!(mZapExisting.equals(other.mZapExisting)))
      return false;
    if (!(mEclipse == other.mEclipse))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mParentDir.hashCode();
      r = r * 37 + mName.hashCode();
      r = r * 37 + (mOmitJsonArgs ? 1 : 0);
      r = r * 37 + mZapExisting.hashCode();
      r = r * 37 + (mEclipse ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mParentDir;
  protected String mName;
  protected boolean mOmitJsonArgs;
  protected File mZapExisting;
  protected boolean mEclipse;
  protected int m__hashcode;

  public static final class Builder extends CreateAppConfig {

    private Builder(CreateAppConfig m) {
      mParentDir = m.mParentDir;
      mName = m.mName;
      mOmitJsonArgs = m.mOmitJsonArgs;
      mZapExisting = m.mZapExisting;
      mEclipse = m.mEclipse;
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
      r.mParentDir = mParentDir;
      r.mName = mName;
      r.mOmitJsonArgs = mOmitJsonArgs;
      r.mZapExisting = mZapExisting;
      r.mEclipse = mEclipse;
      return r;
    }

    public Builder parentDir(File x) {
      mParentDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder omitJsonArgs(boolean x) {
      mOmitJsonArgs = x;
      return this;
    }

    public Builder zapExisting(File x) {
      mZapExisting = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder eclipse(boolean x) {
      mEclipse = x;
      return this;
    }

  }

  public static final CreateAppConfig DEFAULT_INSTANCE = new CreateAppConfig();

  private CreateAppConfig() {
    mParentDir = Files.DEFAULT;
    mName = "";
    mZapExisting = Files.DEFAULT;
  }

}
