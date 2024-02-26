package dev.gen.archive;

import java.io.File;
import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class ArchiveEntry implements AbstractData {

  public int version() {
    return mVersion;
  }

  public File path() {
    return mPath;
  }

  public boolean directory() {
    return mDirectory;
  }

  public List<String> fileExtensions() {
    return mFileExtensions;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "path";
  protected static final String _2 = "directory";
  protected static final String _3 = "file_extensions";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersion);
    m.putUnsafe(_1, mPath.toString());
    m.putUnsafe(_2, mDirectory);
    if (mFileExtensions != null) {
      {
        JSList j = new JSList();
        for (String x : mFileExtensions)
          j.add(x);
        m.put(_3, j);
      }
    }
    return m;
  }

  @Override
  public ArchiveEntry build() {
    return this;
  }

  @Override
  public ArchiveEntry parse(Object obj) {
    return new ArchiveEntry((JSMap) obj);
  }

  private ArchiveEntry(JSMap m) {
    mVersion = m.opt(_0, 0);
    {
      mPath = Files.DEFAULT;
      String x = m.opt(_1, (String) null);
      if (x != null) {
        mPath = new File(x);
      }
    }
    mDirectory = m.opt(_2, false);
    mFileExtensions = DataUtil.parseListOfObjects(m.optJSList(_3), true);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ArchiveEntry))
      return false;
    ArchiveEntry other = (ArchiveEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersion == other.mVersion))
      return false;
    if (!(mPath.equals(other.mPath)))
      return false;
    if (!(mDirectory == other.mDirectory))
      return false;
    if ((mFileExtensions == null) ^ (other.mFileExtensions == null))
      return false;
    if (mFileExtensions != null) {
      if (!(mFileExtensions.equals(other.mFileExtensions)))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion;
      r = r * 37 + mPath.hashCode();
      r = r * 37 + (mDirectory ? 1 : 0);
      if (mFileExtensions != null) {
        for (String x : mFileExtensions)
          if (x != null)
            r = r * 37 + x.hashCode();
      }
      m__hashcode = r;
    }
    return r;
  }

  protected int mVersion;
  protected File mPath;
  protected boolean mDirectory;
  protected List<String> mFileExtensions;
  protected int m__hashcode;

  public static final class Builder extends ArchiveEntry {

    private Builder(ArchiveEntry m) {
      mVersion = m.mVersion;
      mPath = m.mPath;
      mDirectory = m.mDirectory;
      mFileExtensions = DataUtil.mutableCopyOf(m.mFileExtensions);
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
    public ArchiveEntry build() {
      ArchiveEntry r = new ArchiveEntry();
      r.mVersion = mVersion;
      r.mPath = mPath;
      r.mDirectory = mDirectory;
      r.mFileExtensions = DataUtil.immutableCopyOf(mFileExtensions);
      return r;
    }

    public Builder version(int x) {
      mVersion = x;
      return this;
    }

    public Builder path(File x) {
      mPath = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder directory(boolean x) {
      mDirectory = x;
      return this;
    }

    public Builder fileExtensions(List<String> x) {
      mFileExtensions = DataUtil.mutableCopyOf(x);
      return this;
    }

  }

  public static final ArchiveEntry DEFAULT_INSTANCE = new ArchiveEntry();

  private ArchiveEntry() {
    mPath = Files.DEFAULT;
  }

}
