package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class LinodeEntry implements AbstractData {

  public int id() {
    return mId;
  }

  public String label() {
    return mLabel;
  }

  public String ipAddr() {
    return mIpAddr;
  }

  public JSMap linodeInfo() {
    return mLinodeInfo;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "id";
  protected static final String _1 = "label";
  protected static final String _2 = "ip_addr";
  protected static final String _3 = "linode_info";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mId);
    m.putUnsafe(_1, mLabel);
    m.putUnsafe(_2, mIpAddr);
    m.putUnsafe(_3, mLinodeInfo);
    return m;
  }

  @Override
  public LinodeEntry build() {
    return this;
  }

  @Override
  public LinodeEntry parse(Object obj) {
    return new LinodeEntry((JSMap) obj);
  }

  private LinodeEntry(JSMap m) {
    mId = m.opt(_0, 0);
    mLabel = m.opt(_1, "");
    mIpAddr = m.opt(_2, "");
    {
      mLinodeInfo = JSMap.DEFAULT_INSTANCE;
      JSMap x = m.optJSMap(_3);
      if (x != null) {
        mLinodeInfo = x.lock();
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
    if (object == null || !(object instanceof LinodeEntry))
      return false;
    LinodeEntry other = (LinodeEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mId == other.mId))
      return false;
    if (!(mLabel.equals(other.mLabel)))
      return false;
    if (!(mIpAddr.equals(other.mIpAddr)))
      return false;
    if (!(mLinodeInfo.equals(other.mLinodeInfo)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mId;
      r = r * 37 + mLabel.hashCode();
      r = r * 37 + mIpAddr.hashCode();
      r = r * 37 + mLinodeInfo.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected int mId;
  protected String mLabel;
  protected String mIpAddr;
  protected JSMap mLinodeInfo;
  protected int m__hashcode;

  public static final class Builder extends LinodeEntry {

    private Builder(LinodeEntry m) {
      mId = m.mId;
      mLabel = m.mLabel;
      mIpAddr = m.mIpAddr;
      mLinodeInfo = m.mLinodeInfo;
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
    public LinodeEntry build() {
      LinodeEntry r = new LinodeEntry();
      r.mId = mId;
      r.mLabel = mLabel;
      r.mIpAddr = mIpAddr;
      r.mLinodeInfo = mLinodeInfo;
      return r;
    }

    public Builder id(int x) {
      mId = x;
      return this;
    }

    public Builder label(String x) {
      mLabel = (x == null) ? "" : x;
      return this;
    }

    public Builder ipAddr(String x) {
      mIpAddr = (x == null) ? "" : x;
      return this;
    }

    public Builder linodeInfo(JSMap x) {
      mLinodeInfo = (x == null) ? JSMap.DEFAULT_INSTANCE : x;
      return this;
    }

  }

  public static final LinodeEntry DEFAULT_INSTANCE = new LinodeEntry();

  private LinodeEntry() {
    mLabel = "";
    mIpAddr = "";
    mLinodeInfo = JSMap.DEFAULT_INSTANCE;
  }

}
