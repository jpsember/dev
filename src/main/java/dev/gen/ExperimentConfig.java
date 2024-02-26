package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class ExperimentConfig implements AbstractData {

  public File fileArg() {
    return mFileArg;
  }

  public String stringArg() {
    return mStringArg;
  }

  public boolean boolArg() {
    return mBoolArg;
  }

  public int intArg() {
    return mIntArg;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "file_arg";
  protected static final String _1 = "string_arg";
  protected static final String _2 = "bool_arg";
  protected static final String _3 = "int_arg";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mFileArg.toString());
    m.putUnsafe(_1, mStringArg);
    m.putUnsafe(_2, mBoolArg);
    m.putUnsafe(_3, mIntArg);
    return m;
  }

  @Override
  public ExperimentConfig build() {
    return this;
  }

  @Override
  public ExperimentConfig parse(Object obj) {
    return new ExperimentConfig((JSMap) obj);
  }

  private ExperimentConfig(JSMap m) {
    {
      mFileArg = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mFileArg = new File(x);
      }
    }
    mStringArg = m.opt(_1, "hello");
    mBoolArg = m.opt(_2, false);
    mIntArg = m.opt(_3, 42);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ExperimentConfig))
      return false;
    ExperimentConfig other = (ExperimentConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mFileArg.equals(other.mFileArg)))
      return false;
    if (!(mStringArg.equals(other.mStringArg)))
      return false;
    if (!(mBoolArg == other.mBoolArg))
      return false;
    if (!(mIntArg == other.mIntArg))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mFileArg.hashCode();
      r = r * 37 + mStringArg.hashCode();
      r = r * 37 + (mBoolArg ? 1 : 0);
      r = r * 37 + mIntArg;
      m__hashcode = r;
    }
    return r;
  }

  protected File mFileArg;
  protected String mStringArg;
  protected boolean mBoolArg;
  protected int mIntArg;
  protected int m__hashcode;

  public static final class Builder extends ExperimentConfig {

    private Builder(ExperimentConfig m) {
      mFileArg = m.mFileArg;
      mStringArg = m.mStringArg;
      mBoolArg = m.mBoolArg;
      mIntArg = m.mIntArg;
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
    public ExperimentConfig build() {
      ExperimentConfig r = new ExperimentConfig();
      r.mFileArg = mFileArg;
      r.mStringArg = mStringArg;
      r.mBoolArg = mBoolArg;
      r.mIntArg = mIntArg;
      return r;
    }

    public Builder fileArg(File x) {
      mFileArg = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder stringArg(String x) {
      mStringArg = (x == null) ? "hello" : x;
      return this;
    }

    public Builder boolArg(boolean x) {
      mBoolArg = x;
      return this;
    }

    public Builder intArg(int x) {
      mIntArg = x;
      return this;
    }

  }

  public static final ExperimentConfig DEFAULT_INSTANCE = new ExperimentConfig();

  private ExperimentConfig() {
    mFileArg = Files.DEFAULT;
    mStringArg = "hello";
    mIntArg = 42;
  }

}
