package dev.gen;

import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSList;
import js.json.JSMap;

public class LinodeConfig implements AbstractData {

  public String rootPassword() {
    return mRootPassword;
  }

  public String accessToken() {
    return mAccessToken;
  }

  public List<String> authorizedKeys() {
    return mAuthorizedKeys;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "root_password";
  protected static final String _1 = "access_token";
  protected static final String _2 = "authorized_keys";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mRootPassword);
    m.putUnsafe(_1, mAccessToken);
    {
      JSList j = new JSList();
      for (String x : mAuthorizedKeys)
        j.add(x);
      m.put(_2, j);
    }
    return m;
  }

  @Override
  public LinodeConfig build() {
    return this;
  }

  @Override
  public LinodeConfig parse(Object obj) {
    return new LinodeConfig((JSMap) obj);
  }

  private LinodeConfig(JSMap m) {
    mRootPassword = m.opt(_0, "");
    mAccessToken = m.opt(_1, "");
    mAuthorizedKeys = DataUtil.immutableCopyOf(DataUtil.parseListOfObjects(m.optJSList(_2), false)) /*DEBUG*/ ;
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof LinodeConfig))
      return false;
    LinodeConfig other = (LinodeConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mRootPassword.equals(other.mRootPassword)))
      return false;
    if (!(mAccessToken.equals(other.mAccessToken)))
      return false;
    if (!(mAuthorizedKeys.equals(other.mAuthorizedKeys)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mRootPassword.hashCode();
      r = r * 37 + mAccessToken.hashCode();
      for (String x : mAuthorizedKeys)
        if (x != null)
          r = r * 37 + x.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mRootPassword;
  protected String mAccessToken;
  protected List<String> mAuthorizedKeys;
  protected int m__hashcode;

  public static final class Builder extends LinodeConfig {

    private Builder(LinodeConfig m) {
      mRootPassword = m.mRootPassword;
      mAccessToken = m.mAccessToken;
      mAuthorizedKeys = DataUtil.immutableCopyOf(m.mAuthorizedKeys) /*DEBUG*/ ;
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
    public LinodeConfig build() {
      LinodeConfig r = new LinodeConfig();
      r.mRootPassword = mRootPassword;
      r.mAccessToken = mAccessToken;
      r.mAuthorizedKeys = mAuthorizedKeys;
      return r;
    }

    public Builder rootPassword(String x) {
      mRootPassword = (x == null) ? "" : x;
      return this;
    }

    public Builder accessToken(String x) {
      mAccessToken = (x == null) ? "" : x;
      return this;
    }

    public Builder authorizedKeys(List<String> x) {
      mAuthorizedKeys = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final LinodeConfig DEFAULT_INSTANCE = new LinodeConfig();

  private LinodeConfig() {
    mRootPassword = "";
    mAccessToken = "";
    mAuthorizedKeys = DataUtil.emptyList();
  }

}
