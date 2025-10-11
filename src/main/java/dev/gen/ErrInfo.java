package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class ErrInfo implements AbstractData {

  public int id() {
    return mId;
  }

  public String name() {
    return mName;
  }

  public String description() {
    return mDescription;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "id";
  protected static final String _1 = "name";
  protected static final String _2 = "description";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mId);
    m.putUnsafe(_1, mName);
    m.putUnsafe(_2, mDescription);
    return m;
  }

  @Override
  public ErrInfo build() {
    return this;
  }

  @Override
  public ErrInfo parse(Object obj) {
    return new ErrInfo((JSMap) obj);
  }

  private ErrInfo(JSMap m) {
    mId = m.opt(_0, 0);
    mName = m.opt(_1, "");
    mDescription = m.opt(_2, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ErrInfo))
      return false;
    ErrInfo other = (ErrInfo) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mId == other.mId))
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mDescription.equals(other.mDescription)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mId;
      r = r * 37 + mName.hashCode();
      r = r * 37 + mDescription.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mId;
  protected String mName;
  protected String mDescription;
  protected int m__hashcode;

  public static final class Builder extends ErrInfo {

    private Builder(ErrInfo m) {
      mId = m.mId;
      mName = m.mName;
      mDescription = m.mDescription;
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
    public ErrInfo build() {
      ErrInfo r = new ErrInfo();
      r.mId = mId;
      r.mName = mName;
      r.mDescription = mDescription;
      return r;
    }

    public Builder id(int x) {
      mId = x;
      return this;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder description(String x) {
      mDescription = (x == null) ? "" : x;
      return this;
    }

  }

  public static final ErrInfo DEFAULT_INSTANCE = new ErrInfo();

  private ErrInfo() {
    mName = "";
    mDescription = "";
  }

}
