package dev.gen;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class MakeInstallerConfig implements AbstractData {

  public String versionNumber() {
    return mVersionNumber;
  }

  public Map<String, String> programs() {
    return mPrograms;
  }

  public File scriptsDir() {
    return mScriptsDir;
  }

  public File output() {
    return mOutput;
  }

  public String secretPassphrase() {
    return mSecretPassphrase;
  }

  public Map<String, String> sourceVariables() {
    return mSourceVariables;
  }

  public File projectDirectory() {
    return mProjectDirectory;
  }

  public JSList fileList() {
    return mFileList;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "version_number";
  protected static final String _1 = "programs";
  protected static final String _2 = "scripts_dir";
  protected static final String _3 = "output";
  protected static final String _4 = "secret_passphrase";
  protected static final String _5 = "source_variables";
  protected static final String _6 = "project_directory";
  protected static final String _7 = "file_list";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mVersionNumber);
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, String> e : mPrograms.entrySet())
        j.put(e.getKey(), e.getValue());
      m.put(_1, j);
    }
    m.putUnsafe(_2, mScriptsDir.toString());
    m.putUnsafe(_3, mOutput.toString());
    m.putUnsafe(_4, mSecretPassphrase);
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, String> e : mSourceVariables.entrySet())
        j.put(e.getKey(), e.getValue());
      m.put(_5, j);
    }
    m.putUnsafe(_6, mProjectDirectory.toString());
    m.putUnsafe(_7, mFileList);
    return m;
  }

  @Override
  public MakeInstallerConfig build() {
    return this;
  }

  @Override
  public MakeInstallerConfig parse(Object obj) {
    return new MakeInstallerConfig((JSMap) obj);
  }

  private MakeInstallerConfig(JSMap m) {
    mVersionNumber = m.opt(_0, "1.0.0");
    {
      mPrograms = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("programs");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, String> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), (String) e.getValue());
          mPrograms = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
        }
      }
    }
    {
      mScriptsDir = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mScriptsDir = new File(x);
      }
    }
    {
      mOutput = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mOutput = new File(x);
      }
    }
    mSecretPassphrase = m.opt(_4, "");
    {
      mSourceVariables = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("source_variables");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, String> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), (String) e.getValue());
          mSourceVariables = DataUtil.immutableCopyOf(mp) /*DEBUG*/ ;
        }
      }
    }
    {
      mProjectDirectory = Files.DEFAULT;
      String x = m.opt(_6, (String) null);
      if (x != null) {
        mProjectDirectory = new File(x);
      }
    }
    {
      mFileList = JSList.DEFAULT_INSTANCE;
      JSList x = m.optJSList(_7);
      if (x != null) {
        mFileList = x.lock();
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
    if (object == null || !(object instanceof MakeInstallerConfig))
      return false;
    MakeInstallerConfig other = (MakeInstallerConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mVersionNumber.equals(other.mVersionNumber)))
      return false;
    if (!(mPrograms.equals(other.mPrograms)))
      return false;
    if (!(mScriptsDir.equals(other.mScriptsDir)))
      return false;
    if (!(mOutput.equals(other.mOutput)))
      return false;
    if (!(mSecretPassphrase.equals(other.mSecretPassphrase)))
      return false;
    if (!(mSourceVariables.equals(other.mSourceVariables)))
      return false;
    if (!(mProjectDirectory.equals(other.mProjectDirectory)))
      return false;
    if (!(mFileList.equals(other.mFileList)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mVersionNumber.hashCode();
      r = r * 37 + mPrograms.hashCode();
      r = r * 37 + mScriptsDir.hashCode();
      r = r * 37 + mOutput.hashCode();
      r = r * 37 + mSecretPassphrase.hashCode();
      r = r * 37 + mSourceVariables.hashCode();
      r = r * 37 + mProjectDirectory.hashCode();
      r = r * 37 + mFileList.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mVersionNumber;
  protected Map<String, String> mPrograms;
  protected File mScriptsDir;
  protected File mOutput;
  protected String mSecretPassphrase;
  protected Map<String, String> mSourceVariables;
  protected File mProjectDirectory;
  protected JSList mFileList;
  protected int m__hashcode;

  public static final class Builder extends MakeInstallerConfig {

    private Builder(MakeInstallerConfig m) {
      mVersionNumber = m.mVersionNumber;
      mPrograms = DataUtil.immutableCopyOf(m.mPrograms) /*DEBUG*/ ;
      mScriptsDir = m.mScriptsDir;
      mOutput = m.mOutput;
      mSecretPassphrase = m.mSecretPassphrase;
      mSourceVariables = DataUtil.immutableCopyOf(m.mSourceVariables) /*DEBUG*/ ;
      mProjectDirectory = m.mProjectDirectory;
      mFileList = m.mFileList;
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
    public MakeInstallerConfig build() {
      MakeInstallerConfig r = new MakeInstallerConfig();
      r.mVersionNumber = mVersionNumber;
      r.mPrograms = mPrograms;
      r.mScriptsDir = mScriptsDir;
      r.mOutput = mOutput;
      r.mSecretPassphrase = mSecretPassphrase;
      r.mSourceVariables = mSourceVariables;
      r.mProjectDirectory = mProjectDirectory;
      r.mFileList = mFileList;
      return r;
    }

    public Builder versionNumber(String x) {
      mVersionNumber = (x == null) ? "1.0.0" : x;
      return this;
    }

    public Builder programs(Map<String, String> x) {
      mPrograms = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder scriptsDir(File x) {
      mScriptsDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder output(File x) {
      mOutput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder secretPassphrase(String x) {
      mSecretPassphrase = (x == null) ? "" : x;
      return this;
    }

    public Builder sourceVariables(Map<String, String> x) {
      mSourceVariables = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyMap() : x) /*DEBUG*/ ;
      return this;
    }

    public Builder projectDirectory(File x) {
      mProjectDirectory = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder fileList(JSList x) {
      mFileList = (x == null) ? JSList.DEFAULT_INSTANCE : x;
      return this;
    }

  }

  public static final MakeInstallerConfig DEFAULT_INSTANCE = new MakeInstallerConfig();

  private MakeInstallerConfig() {
    mVersionNumber = "1.0.0";
    mPrograms = DataUtil.emptyMap();
    mScriptsDir = Files.DEFAULT;
    mOutput = Files.DEFAULT;
    mSecretPassphrase = "";
    mSourceVariables = DataUtil.emptyMap();
    mProjectDirectory = Files.DEFAULT;
    mFileList = JSList.DEFAULT_INSTANCE;
  }

}
