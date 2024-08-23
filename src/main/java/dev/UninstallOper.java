package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.UninstallConfig;
import js.app.AppOper;
import js.base.BasePrinter;
import js.file.Files;

public class UninstallOper extends AppOper {

  @Override
  public String userCommand() {
    return "uninstall";
  }

  @Override
  public String shortHelp() {
    return "<program> : uninstalls one of Jeff's programs";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("dev uninstall program <program name>");
  }

  @Override
  public UninstallConfig defaultArgs() {
    return UninstallConfig.DEFAULT_INSTANCE;
  }

  @Override
  public UninstallConfig config() {
    if (mConfig == null) {
      mConfig = (UninstallConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var program = config().program(); //readIfMissing(config().program());
    checkNonEmpty(program, "missing argument: program");

    var binDir = binDirectory(); //new File("/usr/local/bin");
    var scriptFile = new File(binDir, program);
    var jarFile = new File(binDir, program + "-1.0-jar-with-dependencies.jar");

    log("deleting program", program, "including driver:", scriptFile, "and jar:", jarFile);

    if (!scriptFile.exists())
      pr("Can't find driver:", scriptFile);
    else
      files().deleteFile(scriptFile);
    if (!jarFile.exists())
      pr("Can't find program:", jarFile);
    else
      files().deleteFile(jarFile);

    log("done!");
  }

  private UninstallConfig mConfig;

  public static File binDirectory() {
    todo("!modify Files version");
    if (sBinDir == null) {
      var d = new File(Files.homeDirectory(), "bin");
      Files.assertDirectoryExists(d, "please create a 'bin' subdirectory in the home directory");
      sBinDir = d;
    }
    return sBinDir;
  }

  private static File sBinDir;
}
