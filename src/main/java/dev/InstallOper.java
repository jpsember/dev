package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.InstallConfig;
import js.app.AppOper;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;

public class InstallOper extends AppOper {

  @Override
  public String userCommand() {
    return "install";
  }

  @Override
  public String shortHelp() {
    return "installs one of my programs from github, including a shell script to run it";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("dev install program <program name> [branch <x>] [repo <x>] [main_class <y>]");
  }

  @Override
  public InstallConfig defaultArgs() {
    return InstallConfig.DEFAULT_INSTANCE;
  }

  @Override
  public InstallConfig config() {
    if (mConfig == null) {
      mConfig = (InstallConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var programName = config().program();
    checkNonEmpty(programName, "missing argument: program");

    var repoName = config().repo();
    if (nullOrEmpty(repoName))
      repoName = programName;

    var tempDir = Files.createTempDir("install_oper");
    var repoDir = new File(tempDir, repoName);
    {
      var s = new SystemCall().directory(tempDir);
      s.setVerbose(verbose());
      var url = "https://github.com/jpsember/" + repoName + ".git";
      log("attempting to clone repo from", url);
      s.arg("git", "clone");
      if (nonEmpty(config().branch()))
        s.arg("-b", config().branch());
      s.arg(url);
      s.assertSuccess();
    }
    final var INFO_SCRIPT_NAME = "install.json";
    var driverSrc = new File(repoDir, INFO_SCRIPT_NAME);
    log("parsing", INFO_SCRIPT_NAME, "(if one was provided)", Files.infoMap(driverSrc));
    var installConfig = Files.parseAbstractDataOpt(InstallConfig.DEFAULT_INSTANCE, driverSrc);

    {
      log("calling 'dev getrepo', verbosity:", verbose());
      var s = new SystemCall().directory(repoDir);
      s.setVerbose(verbose());
      s.arg(Files.programPath("dev"), "getrepo");
      if (verbose())
        s.arg("-v");
      s.assertSuccess();
    }

    File jarFile = null;
    {

      var mvnCommand = "package";

      if (config().force()) {
        log("using force option");
        mvnCommand = "clean package --update-snapshots";
      }

      log("calling 'mvn " + mvnCommand + "'");
      var s = new SystemCall().directory(repoDir);
      s.setVerbose(verbose());

      s.arg(Files.programPath("mvn"));
      for (var x : split(mvnCommand, ' ')) {
        s.arg(x);
      }
      if (config().skipTests())
        s.arg("-Dmaven.test.skip=true");

      if (s.exitCode() != 0)
        setError("Failed to compile", repoName, INDENT, s.systemErr());

      log("looking for target/xxxx-jar-with-dependencies.jar");
      File jarFileSrc = null;
      {
        var dw = new DirWalk(new File(repoDir, "target"));
        dw.withRecurse(false).withExtensions("jar");
        for (var f : dw.files()) {
          var b = Files.basename(f);
          if (!b.endsWith("-jar-with-dependencies"))
            continue;
          log("...found:", f);
          if (jarFileSrc != null)
            setError("multiple output jar files found");
          jarFileSrc = f;
        }
      }
      if (jarFileSrc == null)
        setError("Can't find xxxx-jar-with-dependencies.jar");

      var jarFileDest = new File(Files.binDirectory(), jarFileSrc.getName());
      log("copying build result to:", jarFileDest);
      files().copyFile(jarFileSrc, jarFileDest);
      jarFile = jarFileDest;
    }
    Files.assertNonEmpty(jarFile, "copying jar to the jars directory");

    File scriptFile = null;
    {
      var sb = new StringBuilder();
      sb.append("#!/usr/bin/env sh\n");
      sb.append("set -eu\n");
      sb.append("java -Dfile.encoding=UTF-8 -classpath ");
      sb.append(jarFile);
      sb.append(' ');

      // We have to append the fully qualified class name that contains the main() method, e.g.
      //  js.gitdiff.GitDiffApp
      {
        String mc = config().mainClass();
        if (nullOrEmpty(mc))
          mc = installConfig.mainClass();
        if (nullOrEmpty(mc))
          setError("no main_class provided, either in params or in driver.json");
        sb.append(mc);
        sb.append(' ');
      }

      sb.append("\"$@\"");
      sb.append("\n");
      scriptFile = new File(Files.binDirectory(), programName);
      var content = sb.toString();
      log("writing:", scriptFile, INDENT, content);
      files().writeString(scriptFile, content);
      log("making it executable");
      files().chmod(scriptFile, 755);
    }
    log("done!");
  }

  private InstallConfig mConfig;

}
