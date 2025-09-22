package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class CopyrightConfig implements AbstractData {

  public File sourceDir() {
    return mSourceDir;
  }

  public String fileExtensions() {
    return mFileExtensions;
  }

  public File copyrightTextFile() {
    return mCopyrightTextFile;
  }

  public String headerRegEx() {
    return mHeaderRegEx;
  }

  public boolean removeMessage() {
    return mRemoveMessage;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "source_dir";
  protected static final String _1 = "file_extensions";
  protected static final String _2 = "copyright_text_file";
  protected static final String _3 = "header_reg_ex";
  protected static final String _4 = "remove_message";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mSourceDir.toString());
    m.putUnsafe(_1, mFileExtensions);
    m.putUnsafe(_2, mCopyrightTextFile.toString());
    m.putUnsafe(_3, mHeaderRegEx);
    m.putUnsafe(_4, mRemoveMessage);
    return m;
  }

  @Override
  public CopyrightConfig build() {
    return this;
  }

  @Override
  public CopyrightConfig parse(Object obj) {
    return new CopyrightConfig((JSMap) obj);
  }

  private CopyrightConfig(JSMap m) {
    {
      mSourceDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mSourceDir = new File(x);
      }
    }
    mFileExtensions = m.opt(_1, "");
    {
      mCopyrightTextFile = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mCopyrightTextFile = new File(x);
      }
    }
    mHeaderRegEx = m.opt(_3, "");
    mRemoveMessage = m.opt(_4, false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CopyrightConfig))
      return false;
    CopyrightConfig other = (CopyrightConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSourceDir.equals(other.mSourceDir)))
      return false;
    if (!(mFileExtensions.equals(other.mFileExtensions)))
      return false;
    if (!(mCopyrightTextFile.equals(other.mCopyrightTextFile)))
      return false;
    if (!(mHeaderRegEx.equals(other.mHeaderRegEx)))
      return false;
    if (!(mRemoveMessage == other.mRemoveMessage))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mSourceDir.hashCode();
      r = r * 37 + mFileExtensions.hashCode();
      r = r * 37 + mCopyrightTextFile.hashCode();
      r = r * 37 + mHeaderRegEx.hashCode();
      r = r * 37 + (mRemoveMessage ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mSourceDir;
  protected String mFileExtensions;
  protected File mCopyrightTextFile;
  protected String mHeaderRegEx;
  protected boolean mRemoveMessage;
  protected int m__hashcode;

  public static final class Builder extends CopyrightConfig {

    private Builder(CopyrightConfig m) {
      mSourceDir = m.mSourceDir;
      mFileExtensions = m.mFileExtensions;
      mCopyrightTextFile = m.mCopyrightTextFile;
      mHeaderRegEx = m.mHeaderRegEx;
      mRemoveMessage = m.mRemoveMessage;
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
    public CopyrightConfig build() {
      CopyrightConfig r = new CopyrightConfig();
      r.mSourceDir = mSourceDir;
      r.mFileExtensions = mFileExtensions;
      r.mCopyrightTextFile = mCopyrightTextFile;
      r.mHeaderRegEx = mHeaderRegEx;
      r.mRemoveMessage = mRemoveMessage;
      return r;
    }

    public Builder sourceDir(File x) {
      mSourceDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder fileExtensions(String x) {
      mFileExtensions = (x == null) ? "" : x;
      return this;
    }

    public Builder copyrightTextFile(File x) {
      mCopyrightTextFile = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder headerRegEx(String x) {
      mHeaderRegEx = (x == null) ? "" : x;
      return this;
    }

    public Builder removeMessage(boolean x) {
      mRemoveMessage = x;
      return this;
    }

  }

  public static final CopyrightConfig DEFAULT_INSTANCE = new CopyrightConfig();

  private CopyrightConfig() {
    mSourceDir = Files.DEFAULT;
    mFileExtensions = "";
    mCopyrightTextFile = Files.DEFAULT;
    mHeaderRegEx = "";
  }

}
