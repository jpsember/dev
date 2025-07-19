package dev;

import js.data.DataUtil;
import js.testutil.MyTestCase;

import java.util.List;

import static js.base.Tools.*;

public abstract class DevTestBase extends MyTestCase {

  public final void setOper(String operName) {
    mOperName = operName;
  }

  public final void addArg(Object... args) {
    for (Object a : args) {
      args().add(a.toString());
    }
  }

  public final void runApp() {
    if (verbose())
      addArg("--verbose");
    var app = new Main();
    app.startApplication(DataUtil.toStringArray(args()));
    clearArgs();
    var ex = app.getError();
    if (ex != null) throw ex;
  }

  public final void clearArgs() {
    mArgs = null;
  }

  public void provideArg(String key, Object value) {
    if (!args().contains(key))
      addArg(key, value);
  }

  public final List<String> args() {

    if (mArgs == null) {
      checkState(!nullOrEmpty(mOperName), "no call to setOper()");
      mArgs = arrayList();
      mArgs.add(mOperName);
    }

    return mArgs;
  }

  public final String argValueForKey(String key) {
    var args = args();
    for (int i = 0; i < args.size() - 1; i++) {
      if (args.get(i).equals(key))
        return args.get(i + 1);
    }
    return null;
  }

  private List<String> mArgs;
  private String mOperName;
}
