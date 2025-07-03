package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class PrepConfig implements AbstractData {

  public File dir() {
    return mDir;
  }

  public boolean save() {
    return mSave;
  }

  public boolean restore() {
    return mRestore;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "dir";
  protected static final String _1 = "save";
  protected static final String _2 = "restore";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mDir.toString());
    m.putUnsafe(_1, mSave);
    m.putUnsafe(_2, mRestore);
    return m;
  }

  @Override
  public PrepConfig build() {
    return this;
  }

  @Override
  public PrepConfig parse(Object obj) {
    return new PrepConfig((JSMap) obj);
  }

  private PrepConfig(JSMap m) {
    {
      mDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mDir = new File(x);
      }
    }
    mSave = m.opt(_1, false);
    mRestore = m.opt(_2, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof PrepConfig))
      return false;
    PrepConfig other = (PrepConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mDir.equals(other.mDir)))
      return false;
    if (!(mSave == other.mSave))
      return false;
    if (!(mRestore == other.mRestore))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mDir.hashCode();
      r = r * 37 + (mSave ? 1 : 0);
      r = r * 37 + (mRestore ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mDir;
  protected boolean mSave;
  protected boolean mRestore;
  protected int m__hashcode;

  public static final class Builder extends PrepConfig {

    private Builder(PrepConfig m) {
      mDir = m.mDir;
      mSave = m.mSave;
      mRestore = m.mRestore;
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
    public PrepConfig build() {
      PrepConfig r = new PrepConfig();
      r.mDir = mDir;
      r.mSave = mSave;
      r.mRestore = mRestore;
      return r;
    }

    public Builder dir(File x) {
      mDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder save(boolean x) {
      mSave = x;
      return this;
    }

    public Builder restore(boolean x) {
      mRestore = x;
      return this;
    }

  }

  public static final PrepConfig DEFAULT_INSTANCE = new PrepConfig();

  private PrepConfig() {
    mDir = Files.DEFAULT;
  }

}
