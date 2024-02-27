package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.DriverConfig;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;

public class DriverOper extends AppOper {

  @Override
  public String userCommand() {
    return "driver";
  }

  @Override
  public String getHelpDescription() {
    return "installs one of my programs from github, and generates a driver script";
  }

  @Override
  public DriverConfig defaultArgs() {
    return DriverConfig.DEFAULT_INSTANCE;
  }

  @Override
  public DriverConfig config() {
    if (mConfig == null) {
      mConfig = (DriverConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {

    var name = config().name();
    checkNonEmpty(name, "specify a name");

    log("attempting to clone repo from github.com/jpsember");
    File cloneDir = null;
    String repoName = null;
    var names = list();
    for (var pass = 0; pass < 2; pass++) {
      var repoNameTry = (pass == 0) ? name : "java-" + name;
      {
        var c2 = Files.createTempDir("driver_oper_" + repoNameTry);
        var s = new SystemCall();
        s.setVerbose(verbose());
        s.directory(cloneDir);
        names.add(repoNameTry);
        var url = "https://github.com/jpsember/" + repoNameTry + ".git";
        log("attempting to clone from:", url);
        s.arg("git", "clone", url);
        if (s.exitCode() == 0) {
          cloneDir = c2;
          repoName = repoNameTry;
          break;
        }
      }
    }
    if (repoName == null) {
      setError("Failed to clone repo from github.com/jpsember/", names);
    }
    log("cloned to:", repoName);

    var repoDir = new File(cloneDir, repoName);

    var driverSrc = new File(repoDir, "driver.json");
    log("parsing driver.json (if one was provided)");
    var installConfig = Files.parseAbstractDataOpt(DriverConfig.DEFAULT_INSTANCE, driverSrc);

    File binDir = new File("/usr/local/bin");
    File jarFile = null;
    {
      log("calling 'mvn package'");
      var s = new SystemCall().directory(repoDir);
      s.setVerbose(verbose());
      s.arg("/usr/local/bin/mvn", "package");
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

      var jarDir = new File(binDir, "jpsember_jars");
      log("preparing jar directory:", jarDir);
      files().mkdirs(jarDir);

      var jarFileDest = new File(jarDir, jarFileSrc.getName());
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
      scriptFile = new File(binDir, name);
      var content = sb.toString();
      log("writing:", scriptFile, INDENT, content);
      files().writeString(scriptFile, content);
      log("making it executable");
      files().chmod(scriptFile, 755);
    }
    log("done!");
  }

  private DriverConfig mConfig;

}
