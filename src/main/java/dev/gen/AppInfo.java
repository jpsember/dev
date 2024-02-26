package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class AppInfo implements AbstractData {

  public File dir() {
    return mDir;
  }

  public File pomFile() {
    return mPomFile;
  }

  public File mainFile() {
    return mMainFile;
  }

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

  protected static final String _0 = "dir";
  protected static final String _1 = "pom_file";
  protected static final String _2 = "main_file";
  protected static final String _3 = "name";
  protected static final String _4 = "main_package";
  protected static final String _5 = "main_class_name";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mDir.toString());
    m.putUnsafe(_1, mPomFile.toString());
    m.putUnsafe(_2, mMainFile.toString());
    m.putUnsafe(_3, mName);
    m.putUnsafe(_4, mMainPackage);
    m.putUnsafe(_5, mMainClassName);
    return m;
  }

  @Override
  public AppInfo build() {
    return this;
  }

  @Override
  public AppInfo parse(Object obj) {
    return new AppInfo((JSMap) obj);
  }

  private AppInfo(JSMap m) {
    {
      mDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mDir = new File(x);
      }
    }
    {
      mPomFile = Files.DEFAULT;
      String x = m.opt(_1, (String) null);
      if (x != null) {
        mPomFile = new File(x);
      }
    }
    {
      mMainFile = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mMainFile = new File(x);
      }
    }
    mName = m.opt(_3, "");
    mMainPackage = m.opt(_4, "");
    mMainClassName = m.opt(_5, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof AppInfo))
      return false;
    AppInfo other = (AppInfo) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mDir.equals(other.mDir)))
      return false;
    if (!(mPomFile.equals(other.mPomFile)))
      return false;
    if (!(mMainFile.equals(other.mMainFile)))
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
      r = r * 37 + mDir.hashCode();
      r = r * 37 + mPomFile.hashCode();
      r = r * 37 + mMainFile.hashCode();
      r = r * 37 + mName.hashCode();
      r = r * 37 + mMainPackage.hashCode();
      r = r * 37 + mMainClassName.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected File mDir;
  protected File mPomFile;
  protected File mMainFile;
  protected String mName;
  protected String mMainPackage;
  protected String mMainClassName;
  protected int m__hashcode;

  public static final class Builder extends AppInfo {

    private Builder(AppInfo m) {
      mDir = m.mDir;
      mPomFile = m.mPomFile;
      mMainFile = m.mMainFile;
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
    public AppInfo build() {
      AppInfo r = new AppInfo();
      r.mDir = mDir;
      r.mPomFile = mPomFile;
      r.mMainFile = mMainFile;
      r.mName = mName;
      r.mMainPackage = mMainPackage;
      r.mMainClassName = mMainClassName;
      return r;
    }

    public Builder dir(File x) {
      mDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder pomFile(File x) {
      mPomFile = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder mainFile(File x) {
      mMainFile = (x == null) ? Files.DEFAULT : x;
      return this;
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
      mMainClassName = (x == null) ? "" : x;
      return this;
    }

  }

  public static final AppInfo DEFAULT_INSTANCE = new AppInfo();

  private AppInfo() {
    mDir = Files.DEFAULT;
    mPomFile = Files.DEFAULT;
    mMainFile = Files.DEFAULT;
    mName = "";
    mMainPackage = "";
    mMainClassName = "";
  }

}
