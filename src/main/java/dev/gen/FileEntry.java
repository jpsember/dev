package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class FileEntry implements AbstractData {

  public File sourcePath() {
    return mSourcePath;
  }

  public String key() {
    return mKey;
  }

  public File targetPath() {
    return mTargetPath;
  }

  public boolean encrypt() {
    return mEncrypt;
  }

  public boolean vars() {
    return mVars;
  }

  public String permissions() {
    return mPermissions;
  }

  public int limit() {
    return mLimit;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "source_path";
  protected static final String _1 = "key";
  protected static final String _2 = "target_path";
  protected static final String _3 = "encrypt";
  protected static final String _4 = "vars";
  protected static final String _5 = "permissions";
  protected static final String _6 = "limit";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mSourcePath.toString());
    m.putUnsafe(_1, mKey);
    m.putUnsafe(_2, mTargetPath.toString());
    m.putUnsafe(_3, mEncrypt);
    m.putUnsafe(_4, mVars);
    m.putUnsafe(_5, mPermissions);
    m.putUnsafe(_6, mLimit);
    return m;
  }

  @Override
  public FileEntry build() {
    return this;
  }

  @Override
  public FileEntry parse(Object obj) {
    return new FileEntry((JSMap) obj);
  }

  private FileEntry(JSMap m) {
    {
      mSourcePath = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mSourcePath = new File(x);
      }
    }
    mKey = m.opt(_1, "");
    {
      mTargetPath = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mTargetPath = new File(x);
      }
    }
    mEncrypt = m.opt(_3, false);
    mVars = m.opt(_4, false);
    mPermissions = m.opt(_5, "");
    mLimit = m.opt(_6, 0);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof FileEntry))
      return false;
    FileEntry other = (FileEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSourcePath.equals(other.mSourcePath)))
      return false;
    if (!(mKey.equals(other.mKey)))
      return false;
    if (!(mTargetPath.equals(other.mTargetPath)))
      return false;
    if (!(mEncrypt == other.mEncrypt))
      return false;
    if (!(mVars == other.mVars))
      return false;
    if (!(mPermissions.equals(other.mPermissions)))
      return false;
    if (!(mLimit == other.mLimit))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mSourcePath.hashCode();
      r = r * 37 + mKey.hashCode();
      r = r * 37 + mTargetPath.hashCode();
      r = r * 37 + (mEncrypt ? 1 : 0);
      r = r * 37 + (mVars ? 1 : 0);
      r = r * 37 + mPermissions.hashCode();
      r = r * 37 + mLimit;
      m__hashcode = r;
    }
    return r;
  }

  protected File mSourcePath;
  protected String mKey;
  protected File mTargetPath;
  protected boolean mEncrypt;
  protected boolean mVars;
  protected String mPermissions;
  protected int mLimit;
  protected int m__hashcode;

  public static final class Builder extends FileEntry {

    private Builder(FileEntry m) {
      mSourcePath = m.mSourcePath;
      mKey = m.mKey;
      mTargetPath = m.mTargetPath;
      mEncrypt = m.mEncrypt;
      mVars = m.mVars;
      mPermissions = m.mPermissions;
      mLimit = m.mLimit;
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
    public FileEntry build() {
      FileEntry r = new FileEntry();
      r.mSourcePath = mSourcePath;
      r.mKey = mKey;
      r.mTargetPath = mTargetPath;
      r.mEncrypt = mEncrypt;
      r.mVars = mVars;
      r.mPermissions = mPermissions;
      r.mLimit = mLimit;
      return r;
    }

    public Builder sourcePath(File x) {
      mSourcePath = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder key(String x) {
      mKey = (x == null) ? "" : x;
      return this;
    }

    public Builder targetPath(File x) {
      mTargetPath = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder encrypt(boolean x) {
      mEncrypt = x;
      return this;
    }

    public Builder vars(boolean x) {
      mVars = x;
      return this;
    }

    public Builder permissions(String x) {
      mPermissions = (x == null) ? "" : x;
      return this;
    }

    public Builder limit(int x) {
      mLimit = x;
      return this;
    }

  }

  public static final FileEntry DEFAULT_INSTANCE = new FileEntry();

  private FileEntry() {
    mSourcePath = Files.DEFAULT;
    mKey = "";
    mTargetPath = Files.DEFAULT;
    mPermissions = "";
  }

}
