package dev.gen;

import java.util.Arrays;
import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSList;
import js.json.JSMap;

public class DeployInfo implements AbstractData {

  public String version() {
    return mVersion;
  }

  public List<FileEntry> files() {
    return mFiles;
  }

  public List<FileEntry> createDirs() {
    return mCreateDirs;
  }

  public List<String> variables() {
    return mVariables;
  }

  public byte[] checkPassphrase() {
    return mCheckPassphrase;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "files";
  protected static final String _2 = "create_dirs";
  protected static final String _3 = "variables";
  protected static final String _4 = "check_passphrase";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersion);
    {
      JSList j = new JSList();
      for (FileEntry x : mFiles)
        j.add(x.toJson());
      m.put(_1, j);
    }
    {
      JSList j = new JSList();
      for (FileEntry x : mCreateDirs)
        j.add(x.toJson());
      m.put(_2, j);
    }
    {
      JSList j = new JSList();
      for (String x : mVariables)
        j.add(x);
      m.put(_3, j);
    }
    m.putUnsafe(_4, DataUtil.encodeBase64Maybe(mCheckPassphrase));
    return m;
  }

  @Override
  public DeployInfo build() {
    return this;
  }

  @Override
  public DeployInfo parse(Object obj) {
    return new DeployInfo((JSMap) obj);
  }

  private DeployInfo(JSMap m) {
    mVersion = m.opt(_0, "");
    mFiles = DataUtil.parseListOfObjects(FileEntry.DEFAULT_INSTANCE, m.optJSList(_1), false);
    mCreateDirs = DataUtil.parseListOfObjects(FileEntry.DEFAULT_INSTANCE, m.optJSList(_2), false);
    mVariables = DataUtil.immutableCopyOf(DataUtil.parseListOfObjects(m.optJSList(_3), false)) /*DEBUG*/ ;
    {
      mCheckPassphrase = DataUtil.EMPTY_BYTE_ARRAY;
      Object x = m.optUnsafe(_4);
      if (x != null) {
        mCheckPassphrase = DataUtil.parseBytesFromArrayOrBase64(x);
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
    if (object == null || !(object instanceof DeployInfo))
      return false;
    DeployInfo other = (DeployInfo) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersion.equals(other.mVersion)))
      return false;
    if (!(mFiles.equals(other.mFiles)))
      return false;
    if (!(mCreateDirs.equals(other.mCreateDirs)))
      return false;
    if (!(mVariables.equals(other.mVariables)))
      return false;
    if (!(Arrays.equals(mCheckPassphrase, other.mCheckPassphrase)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion.hashCode();
      for (FileEntry x : mFiles)
        if (x != null)
          r = r * 37 + x.hashCode();
      for (FileEntry x : mCreateDirs)
        if (x != null)
          r = r * 37 + x.hashCode();
      for (String x : mVariables)
        if (x != null)
          r = r * 37 + x.hashCode();
      r = r * 37 + Arrays.hashCode(mCheckPassphrase);
      m__hashcode = r;
    }
    return r;
  }

  protected String mVersion;
  protected List<FileEntry> mFiles;
  protected List<FileEntry> mCreateDirs;
  protected List<String> mVariables;
  protected byte[] mCheckPassphrase;
  protected int m__hashcode;

  public static final class Builder extends DeployInfo {

    private Builder(DeployInfo m) {
      mVersion = m.mVersion;
      mFiles = DataUtil.immutableCopyOf(m.mFiles) /*DEBUG*/ ;
      mCreateDirs = DataUtil.immutableCopyOf(m.mCreateDirs) /*DEBUG*/ ;
      mVariables = DataUtil.immutableCopyOf(m.mVariables) /*DEBUG*/ ;
      mCheckPassphrase = m.mCheckPassphrase;
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
    public DeployInfo build() {
      DeployInfo r = new DeployInfo();
      r.mVersion = mVersion;
      r.mFiles = mFiles;
      r.mCreateDirs = mCreateDirs;
      r.mVariables = mVariables;
      r.mCheckPassphrase = mCheckPassphrase;
      return r;
    }

    public Builder version(String x) {
      mVersion = (x == null) ? "" : x;
      return this;
    }

    public Builder files(List<FileEntry> x) {
      mFiles = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder createDirs(List<FileEntry> x) {
      mCreateDirs = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder variables(List<String> x) {
      mVariables = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder checkPassphrase(byte[] x) {
      mCheckPassphrase = (x == null) ? DataUtil.EMPTY_BYTE_ARRAY : x;
      return this;
    }

  }

  public static final DeployInfo DEFAULT_INSTANCE = new DeployInfo();

  private DeployInfo() {
    mVersion = "";
    mFiles = DataUtil.emptyList();
    mCreateDirs = DataUtil.emptyList();
    mVariables = DataUtil.emptyList();
    mCheckPassphrase = DataUtil.EMPTY_BYTE_ARRAY;
  }

}
