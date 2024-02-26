package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class DependencyEntry implements AbstractData {

  public String group() {
    return mGroup;
  }

  public String artifact() {
    return mArtifact;
  }

  public String type() {
    return mType;
  }

  public String version() {
    return mVersion;
  }

  public String phase() {
    return mPhase;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "group";
  protected static final String _1 = "artifact";
  protected static final String _2 = "type";
  protected static final String _3 = "version";
  protected static final String _4 = "phase";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mGroup);
    m.putUnsafe(_1, mArtifact);
    m.putUnsafe(_2, mType);
    m.putUnsafe(_3, mVersion);
    m.putUnsafe(_4, mPhase);
    return m;
  }

  @Override
  public DependencyEntry build() {
    return this;
  }

  @Override
  public DependencyEntry parse(Object obj) {
    return new DependencyEntry((JSMap) obj);
  }

  private DependencyEntry(JSMap m) {
    mGroup = m.opt(_0, "");
    mArtifact = m.opt(_1, "");
    mType = m.opt(_2, "");
    mVersion = m.opt(_3, "");
    mPhase = m.opt(_4, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof DependencyEntry))
      return false;
    DependencyEntry other = (DependencyEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mGroup.equals(other.mGroup)))
      return false;
    if (!(mArtifact.equals(other.mArtifact)))
      return false;
    if (!(mType.equals(other.mType)))
      return false;
    if (!(mVersion.equals(other.mVersion)))
      return false;
    if (!(mPhase.equals(other.mPhase)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mGroup.hashCode();
      r = r * 37 + mArtifact.hashCode();
      r = r * 37 + mType.hashCode();
      r = r * 37 + mVersion.hashCode();
      r = r * 37 + mPhase.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mGroup;
  protected String mArtifact;
  protected String mType;
  protected String mVersion;
  protected String mPhase;
  protected int m__hashcode;

  public static final class Builder extends DependencyEntry {

    private Builder(DependencyEntry m) {
      mGroup = m.mGroup;
      mArtifact = m.mArtifact;
      mType = m.mType;
      mVersion = m.mVersion;
      mPhase = m.mPhase;
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
    public DependencyEntry build() {
      DependencyEntry r = new DependencyEntry();
      r.mGroup = mGroup;
      r.mArtifact = mArtifact;
      r.mType = mType;
      r.mVersion = mVersion;
      r.mPhase = mPhase;
      return r;
    }

    public Builder group(String x) {
      mGroup = (x == null) ? "" : x;
      return this;
    }

    public Builder artifact(String x) {
      mArtifact = (x == null) ? "" : x;
      return this;
    }

    public Builder type(String x) {
      mType = (x == null) ? "" : x;
      return this;
    }

    public Builder version(String x) {
      mVersion = (x == null) ? "" : x;
      return this;
    }

    public Builder phase(String x) {
      mPhase = (x == null) ? "" : x;
      return this;
    }

  }

  public static final DependencyEntry DEFAULT_INSTANCE = new DependencyEntry();

  private DependencyEntry() {
    mGroup = "";
    mArtifact = "";
    mType = "";
    mVersion = "";
    mPhase = "";
  }

}
