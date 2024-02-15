package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.LinodeConfig;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class LinodeOper extends AppOper {

  public static final String TEST_ARGS = "--exceptions -v linode create label cpu";

  @Override
  public String userCommand() {
    return "linode";
  }

  @Override
  public String getHelpDescription() {
    return "no help is available for operation";
  }

  @Override
  public LinodeConfig defaultArgs() {
    return LinodeConfig.DEFAULT_INSTANCE;
  }

  @Override
  public LinodeConfig config() {
    if (mConfig == null) {
      var c = ((LinodeConfig) super.config()).toBuilder();
      File secrets = new File("linode_secrets.json");
      if (!secrets.exists())
        secrets = new File(Files.homeDirectory(), ".ssh/linode_secrets.json");
      log("looking for secrets in:", secrets, "exists:", secrets.exists());
      if (secrets.exists()) {
        var s = Files.parseAbstractData(LinodeConfig.DEFAULT_INSTANCE, secrets);
        if (c.accessToken().isEmpty())
          c.accessToken(s.accessToken());
        if (c.rootPassword().isEmpty())
          c.rootPassword(s.rootPassword());
      }
      if (c.accessToken().isEmpty())
        throw setError("No access_token provided");

      mConfig = c.build();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var a = cmdLineArgs();

    if (!a.hasNextArg()) {
      pr("(no Linode commands specified)");
    }

    while (a.hasNextArg()) {
      var cmd = a.nextArg();
      switch (cmd) {
      default:
        throw setError("Unrecognized command:", cmd);
      case "create":
        createLinode();
        break;
      }
    }
  }

  private void createLinode() {
    var a = cmdLineArgs();

    var label = a.nextArgIf("label", "");
    var gpu = a.nextArgIf("gpu");
    if (gpu)
      todo("support gpu");

    if (label.isEmpty())
      throw setError("please specify a label for the linode");

    log("config:", INDENT, config());

    var m = map();

    m.put("authorized_users", list().add("jpsember")) //
        .put("authorized_keys", list().add(
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDIdpbQF+FOu0s6OBT0IivYKyjszCPSN9is5IVpBQ6CHX4ZVYeYowhVxLsnwB4RWj/t8sEsQTGqD9V0NscdGongOST6344RcVmAuRYPaOUY9LqqKQnojrYtCWGfMDAmjadtUJqfpxhs2GwFgSS4u9CsATjAhoso5gpk4RdBTJghck1qLGMFeEg0pTUpOJ6Rq8NEjmlLrLVHi1obgLhuZANgqJcNhrfWiUPKQXoMXNXWJDkqMdONxphJe7Fv/y6GRI2tYktElKS68XQ7QVA+/PpNDUcW5KNHS9uf1Az7jb8PuWYzjn6rPpF4O8fnbdxfsK2X1HXN9vn+I8XNS0Mkq/cL Jeff's primary public RSA key")) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", "cheapcpu") //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;

    callLinode("instances", m);
    //    
    //    var sc = new SystemCall();
    //    sc.setVerbose(verbose());
    //
    //    var m = map();
    //    m.put("authorized_users", list().add("jpsember")) //
    //        .put("backups_enabled", false) //
    //        .put("booted", true) //
    //        .put("image", "linode/ubuntu20.04") //
    //        .put("label", "cheapcpu") //
    //        .put("private_ip", false)//
    //        .put("region", "us-sea")//
    //        .put("root_pass", config().rootPassword()) //
    //        .put("tags", list()) //
    //        .put("type", "g6-nanode-1") //
    //    ;
    //
    //    sc.arg("curl", //
    //        "-s", // suppress progress bar
    //        "-H", "Content-Type: application/json", //
    //        "-H", "Authorization: Bearer " + config().accessToken() + "DISABLED", //
    //        "-X", "POST", "-d", m, //
    //        "https://api.linode.com/v4/linode/instances");
    //
    //    if (sc.exitCode() != 0) {
    //      alert("got exit code", sc.exitCode(), INDENT, sc.systemErr());
    //    }
    //
    //    sc.assertSuccess();

    //var out = new JSMap(sc.systemOut());

    verifyOk();
  }

  private boolean verifyOk() {
    var errors = mErrors;
    if (!errors.isEmpty())
      setError("Errors in system call!", INDENT, errors);
    return true;
  }

  private void callLinode(String endpoint, JSMap m) {

    mErrors = null;
    mSysCallOutput = null;

    if (m == null)
      m = map();

    var sc = new SystemCall();
    sc.setVerbose(verbose());

    m.put("authorized_users", list().add("jpsember")) //
        .put("backups_enabled", false) //
        .put("booted", true) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", "cheapcpu") //
        .put("private_ip", false)//
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("tags", list()) //
        .put("type", "g6-nanode-1") //
    ;
    mSysCall = m;

    sc.arg("curl", //
        "-s", // suppress progress bar
        "-H", "Content-Type: application/json", //
        "-H", "Authorization: Bearer " + config().accessToken() + "DISABLED", //
        "-X", "POST", "-d", m, //
        "https://api.linode.com/v4/linode/" + endpoint);

    if (sc.exitCode() != 0) {
      alert("got exit code", sc.exitCode(), INDENT, sc.systemErr());
    }

    sc.assertSuccess();

    var out = new JSMap(sc.systemOut());
    mSysCallOutput = out;
    log("system call output:", INDENT, out);

    mErrors = out.optJSListOrEmpty("errors");
    if (!mErrors.isEmpty())
      log("Errors occurred in the system call:", INDENT, out);
  }

  private LinodeConfig mConfig;
  private JSList mErrors;
  private JSMap mSysCallOutput;
  private JSMap mSysCall;

}
