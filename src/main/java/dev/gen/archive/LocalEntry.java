package dev.gen.archive;

import js.data.AbstractData;
import js.json.JSMap;

public class LocalEntry implements AbstractData {

  public int version() {
    return mVersion;
  }

  public boolean offload() {
    return mOffload;
  }

  public Oper pending() {
    return mPending;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version";
  protected static final String _1 = "offload";
  protected static final String _2 = "pending";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersion);
    m.putUnsafe(_1, mOffload);
    m.putUnsafe(_2, mPending.toString().toLowerCase());
    return m;
  }

  @Override
  public LocalEntry build() {
    return this;
  }

  @Override
  public LocalEntry parse(Object obj) {
    return new LocalEntry((JSMap) obj);
  }

  private LocalEntry(JSMap m) {
    mVersion = m.opt(_0, 0);
    mOffload = m.opt(_1, false);
    {
      String x = m.opt(_2, "");
      mPending = x.isEmpty() ? Oper.DEFAULT_INSTANCE : Oper.valueOf(x.toUpperCase());
    }
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof LocalEntry))
      return false;
    LocalEntry other = (LocalEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersion == other.mVersion))
      return false;
    if (!(mOffload == other.mOffload))
      return false;
    if (!(mPending.equals(other.mPending)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersion;
      r = r * 37 + (mOffload ? 1 : 0);
      r = r * 37 + mPending.ordinal();
      m__hashcode = r;
    }
    return r;
  }

  protected int mVersion;
  protected boolean mOffload;
  protected Oper mPending;
  protected int m__hashcode;

  public static final class Builder extends LocalEntry {

    private Builder(LocalEntry m) {
      mVersion = m.mVersion;
      mOffload = m.mOffload;
      mPending = m.mPending;
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
    public LocalEntry build() {
      LocalEntry r = new LocalEntry();
      r.mVersion = mVersion;
      r.mOffload = mOffload;
      r.mPending = mPending;
      return r;
    }

    public Builder version(int x) {
      mVersion = x;
      return this;
    }

    public Builder offload(boolean x) {
      mOffload = x;
      return this;
    }

    public Builder pending(Oper x) {
      mPending = (x == null) ? Oper.DEFAULT_INSTANCE : x;
      return this;
    }

  }

  public static final LocalEntry DEFAULT_INSTANCE = new LocalEntry();

  private LocalEntry() {
    mPending = Oper.DEFAULT_INSTANCE;
  }

}
