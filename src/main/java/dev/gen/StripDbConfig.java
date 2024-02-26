package dev.gen;

import java.io.File;
import java.util.List;
import js.base.Tools;
import js.data.AbstractData;
import js.data.DataUtil;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class StripDbConfig implements AbstractData {

  public File file() {
    return mFile;
  }

  public List<String> exprs() {
    return mExprs;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "file";
  protected static final String _1 = "exprs";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mFile.toString());
    {
      JSList j = new JSList();
      for (String x : mExprs)
        j.add(x);
      m.put(_1, j);
    }
    return m;
  }

  @Override
  public StripDbConfig build() {
    return this;
  }

  @Override
  public StripDbConfig parse(Object obj) {
    return new StripDbConfig((JSMap) obj);
  }

  private StripDbConfig(JSMap m) {
    {
      mFile = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mFile = new File(x);
      }
    }
    mExprs = DataUtil.immutableCopyOf(DataUtil.parseListOfObjects(m.optJSList(_1), false)) /*DEBUG*/ ;
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof StripDbConfig))
      return false;
    StripDbConfig other = (StripDbConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mFile.equals(other.mFile)))
      return false;
    if (!(mExprs.equals(other.mExprs)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mFile.hashCode();
      for (String x : mExprs)
        if (x != null)
          r = r * 37 + x.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected File mFile;
  protected List<String> mExprs;
  protected int m__hashcode;

  public static final class Builder extends StripDbConfig {

    private Builder(StripDbConfig m) {
      mFile = m.mFile;
      mExprs = DataUtil.immutableCopyOf(m.mExprs) /*DEBUG*/ ;
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
    public StripDbConfig build() {
      StripDbConfig r = new StripDbConfig();
      r.mFile = mFile;
      r.mExprs = mExprs;
      return r;
    }

    public Builder file(File x) {
      mFile = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder exprs(List<String> x) {
      mExprs = DataUtil.immutableCopyOf((x == null) ? _D1 : x) /*DEBUG*/ ;
      return this;
    }

  }

  private static final List<String> _D1 = Tools.arrayList("db","checkState","checkArgument");

  public static final StripDbConfig DEFAULT_INSTANCE = new StripDbConfig();

  private StripDbConfig() {
    mFile = Files.DEFAULT;
    mExprs = _D1;
  }

}
