package dev.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class CaptureConfig implements AbstractData {

  public int devices() {
    return mDevices;
  }

  public int maxScreenshots() {
    return mMaxScreenshots;
  }

  public int secondsBetweenShots() {
    return mSecondsBetweenShots;
  }

  public File imageDirectory() {
    return mImageDirectory;
  }

  public String imagePrefix() {
    return mImagePrefix;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "devices";
  protected static final String _1 = "max_screenshots";
  protected static final String _2 = "seconds_between_shots";
  protected static final String _3 = "image_directory";
  protected static final String _4 = "image_prefix";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mDevices);
    m.putUnsafe(_1, mMaxScreenshots);
    m.putUnsafe(_2, mSecondsBetweenShots);
    m.putUnsafe(_3, mImageDirectory.toString());
    m.putUnsafe(_4, mImagePrefix);
    return m;
  }

  @Override
  public CaptureConfig build() {
    return this;
  }

  @Override
  public CaptureConfig parse(Object obj) {
    return new CaptureConfig((JSMap) obj);
  }

  private CaptureConfig(JSMap m) {
    mDevices = m.opt(_0, 2);
    mMaxScreenshots = m.opt(_1, 1500);
    mSecondsBetweenShots = m.opt(_2, 60);
    {
      mImageDirectory = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mImageDirectory = new File(x);
      }
    }
    mImagePrefix = m.opt(_4, "_screen_capture_");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CaptureConfig))
      return false;
    CaptureConfig other = (CaptureConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mDevices == other.mDevices))
      return false;
    if (!(mMaxScreenshots == other.mMaxScreenshots))
      return false;
    if (!(mSecondsBetweenShots == other.mSecondsBetweenShots))
      return false;
    if (!(mImageDirectory.equals(other.mImageDirectory)))
      return false;
    if (!(mImagePrefix.equals(other.mImagePrefix)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mDevices;
      r = r * 37 + mMaxScreenshots;
      r = r * 37 + mSecondsBetweenShots;
      r = r * 37 + mImageDirectory.hashCode();
      r = r * 37 + mImagePrefix.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mDevices;
  protected int mMaxScreenshots;
  protected int mSecondsBetweenShots;
  protected File mImageDirectory;
  protected String mImagePrefix;
  protected int m__hashcode;

  public static final class Builder extends CaptureConfig {

    private Builder(CaptureConfig m) {
      mDevices = m.mDevices;
      mMaxScreenshots = m.mMaxScreenshots;
      mSecondsBetweenShots = m.mSecondsBetweenShots;
      mImageDirectory = m.mImageDirectory;
      mImagePrefix = m.mImagePrefix;
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
    public CaptureConfig build() {
      CaptureConfig r = new CaptureConfig();
      r.mDevices = mDevices;
      r.mMaxScreenshots = mMaxScreenshots;
      r.mSecondsBetweenShots = mSecondsBetweenShots;
      r.mImageDirectory = mImageDirectory;
      r.mImagePrefix = mImagePrefix;
      return r;
    }

    public Builder devices(int x) {
      mDevices = x;
      return this;
    }

    public Builder maxScreenshots(int x) {
      mMaxScreenshots = x;
      return this;
    }

    public Builder secondsBetweenShots(int x) {
      mSecondsBetweenShots = x;
      return this;
    }

    public Builder imageDirectory(File x) {
      mImageDirectory = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder imagePrefix(String x) {
      mImagePrefix = (x == null) ? "_screen_capture_" : x;
      return this;
    }

  }

  public static final CaptureConfig DEFAULT_INSTANCE = new CaptureConfig();

  private CaptureConfig() {
    mDevices = 2;
    mMaxScreenshots = 1500;
    mSecondsBetweenShots = 60;
    mImageDirectory = Files.DEFAULT;
    mImagePrefix = "_screen_capture_";
  }

}
