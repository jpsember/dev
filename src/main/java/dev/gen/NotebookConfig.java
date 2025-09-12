package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class NotebookConfig implements AbstractData {

  public File input() {
    return mInput;
  }

  public File output() {
    return mOutput;
  }

  public boolean prettyPrint() {
    return mPrettyPrint;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "input";
  protected static final String _1 = "output";
  protected static final String _2 = "pretty_print";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mInput.toString());
    m.putUnsafe(_1, mOutput.toString());
    m.putUnsafe(_2, mPrettyPrint);
    return m;
  }

  @Override
  public NotebookConfig build() {
    return this;
  }

  @Override
  public NotebookConfig parse(Object obj) {
    return new NotebookConfig((JSMap) obj);
  }

  private NotebookConfig(JSMap m) {
    {
      mInput = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mInput = new File(x);
      }
    }
    {
      mOutput = Files.DEFAULT;
      String x = m.opt(_1, (String) null);
      if (x != null) {
        mOutput = new File(x);
      }
    }
    mPrettyPrint = m.opt(_2, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof NotebookConfig))
      return false;
    NotebookConfig other = (NotebookConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mInput.equals(other.mInput)))
      return false;
    if (!(mOutput.equals(other.mOutput)))
      return false;
    if (!(mPrettyPrint == other.mPrettyPrint))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mInput.hashCode();
      r = r * 37 + mOutput.hashCode();
      r = r * 37 + (mPrettyPrint ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mInput;
  protected File mOutput;
  protected boolean mPrettyPrint;
  protected int m__hashcode;

  public static final class Builder extends NotebookConfig {

    private Builder(NotebookConfig m) {
      mInput = m.mInput;
      mOutput = m.mOutput;
      mPrettyPrint = m.mPrettyPrint;
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
    public NotebookConfig build() {
      NotebookConfig r = new NotebookConfig();
      r.mInput = mInput;
      r.mOutput = mOutput;
      r.mPrettyPrint = mPrettyPrint;
      return r;
    }

    public Builder input(File x) {
      mInput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder output(File x) {
      mOutput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder prettyPrint(boolean x) {
      mPrettyPrint = x;
      return this;
    }

  }

  public static final NotebookConfig DEFAULT_INSTANCE = new NotebookConfig();

  private NotebookConfig() {
    mInput = Files.DEFAULT;
    mOutput = Files.DEFAULT;
  }

}
