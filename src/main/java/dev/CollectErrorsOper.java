package dev;

import static js.base.Tools.*;

import dev.gen.CollectErrorsConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.file.DirWalk;
import js.file.Files;

public class CollectErrorsOper extends AppOper {

  @Override
  public String userCommand() {
    return "collecterrors";
  }


  @Override
  public String shortHelp() {
    return "no help is available for operation";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("***", "** no 'long' help available yet");
    b.pr(hf);
    b.br();
  }

  @Override
  public CollectErrorsConfig defaultArgs() {
    return CollectErrorsConfig.DEFAULT_INSTANCE;
  }

  @Override
  public CollectErrorsConfig config() {
    if (mConfig == null) {
      mConfig = (CollectErrorsConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var f = Files.assertDirectoryExists( config().input(), "input directory");

    var ext = arrayList("java","rs","py");
    var dw = new DirWalk(f).withRecurse(true).withExtensions(ext);
    for (var w : dw.files()) {
      var txt = Files.readString(w);

    }

  }

  private CollectErrorsConfig mConfig;

}
