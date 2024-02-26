package dev.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class NewOperConfig implements AbstractData {

  public String name() {
    return mName;
  }

  public String configName() {
    return mConfigName;
  }

  public String helpDescription() {
    return mHelpDescription;
  }

  public String userCommand() {
    return mUserCommand;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";
  protected static final String _1 = "config_name";
  protected static final String _2 = "help_description";
  protected static final String _3 = "user_command";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    m.putUnsafe(_1, mConfigName);
    m.putUnsafe(_2, mHelpDescription);
    m.putUnsafe(_3, mUserCommand);
    return m;
  }

  @Override
  public NewOperConfig build() {
    return this;
  }

  @Override
  public NewOperConfig parse(Object obj) {
    return new NewOperConfig((JSMap) obj);
  }

  private NewOperConfig(JSMap m) {
    mName = m.opt(_0, "");
    mConfigName = m.opt(_1, "");
    mHelpDescription = m.opt(_2, "no help is available for operation");
    mUserCommand = m.opt(_3, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof NewOperConfig))
      return false;
    NewOperConfig other = (NewOperConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mConfigName.equals(other.mConfigName)))
      return false;
    if (!(mHelpDescription.equals(other.mHelpDescription)))
      return false;
    if (!(mUserCommand.equals(other.mUserCommand)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      r = r * 37 + mConfigName.hashCode();
      r = r * 37 + mHelpDescription.hashCode();
      r = r * 37 + mUserCommand.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected String mConfigName;
  protected String mHelpDescription;
  protected String mUserCommand;
  protected int m__hashcode;

  public static final class Builder extends NewOperConfig {

    private Builder(NewOperConfig m) {
      mName = m.mName;
      mConfigName = m.mConfigName;
      mHelpDescription = m.mHelpDescription;
      mUserCommand = m.mUserCommand;
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
    public NewOperConfig build() {
      NewOperConfig r = new NewOperConfig();
      r.mName = mName;
      r.mConfigName = mConfigName;
      r.mHelpDescription = mHelpDescription;
      r.mUserCommand = mUserCommand;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder configName(String x) {
      mConfigName = (x == null) ? "" : x;
      return this;
    }

    public Builder helpDescription(String x) {
      mHelpDescription = (x == null) ? "no help is available for operation" : x;
      return this;
    }

    public Builder userCommand(String x) {
      mUserCommand = (x == null) ? "" : x;
      return this;
    }

  }

  public static final NewOperConfig DEFAULT_INSTANCE = new NewOperConfig();

  private NewOperConfig() {
    mName = "";
    mConfigName = "";
    mHelpDescription = "no help is available for operation";
    mUserCommand = "";
  }

}
