package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.LinodeConfig;
import js.app.AppOper;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class LinodeOper extends AppOper {

  @Override
  public String userCommand() {
    return "linode";
  }

  @Override
  public String getHelpDescription() {
    return "no help is available for operation";
  }

  @Override
  protected void getOperSpecificHelp(BasePrinter b) {
    b.pr("list [detail]");
    b.pr("create <label> [gpu]  ");
    b.pr("delete <label>");
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
      log("looking for secrets file #1:", INDENT, Files.infoMap(secrets));
      if (!secrets.exists()) {
        secrets = new File(Files.homeDirectory(), ".ssh/linode_secrets.json");
        log("looking for secrets file #2:", INDENT, Files.infoMap(secrets));
      }
      log("looking for secrets in:", secrets, "exists:", secrets.exists());
      if (secrets.exists()) {
        var s = Files.parseAbstractData(LinodeConfig.DEFAULT_INSTANCE, secrets);
        if (c.accessToken().isEmpty())
          c.accessToken(s.accessToken());
        if (c.rootPassword().isEmpty())
          c.rootPassword(s.rootPassword());
        if (c.authorizedKeys().isEmpty())
          c.authorizedKeys(s.authorizedKeys());
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
      case "list":
        listNodes();
        break;
      case "delete":
        deleteLinode();
        break;
      }
    }
  }

  private void createLinode() {
    var a = cmdLineArgs();
    var label = a.nextArg("");
    var gpu = a.nextArgIf("gpu");
    if (gpu)
      todo("support gpu");

    if (label.isEmpty())
      throw setError("please specify a label for the linode");
    if (getLinodeId(label) != 0)
      throw setError("linode with label", label, "already exists");

    log("config:", INDENT, config());

    var m = map();
    m //
        // If I add an authorized user OTHER than 'jpsember', it fails with an error "Username XXX is invalid.".
        // Is this because the authorized keys is somehow tied to that name?

        // Ah ha: at https://www.linode.com/docs/api/linode-instances/#linode-create, it says:
        //
        // "These users must have an SSH Key associated with your Profile first. 
        //  See SSH Key Add (POST /profile/sshkeys) for more information."
        //
        .put("authorized_users", list().add("jeff")) //
        .put("authorized_keys", JSList.with(config().authorizedKeys())) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", label) //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;

    callLinode("POST", "instances", m);
    verifyOk();
    discardIdMap();
  }

  private void listNodes() {
    var detail = cmdLineArgs().nextArgIf("detail");

    var m2 = map();
    var m = labelToIdMap();
    for (String label : m.keySet()) {
      var m3 = m.getMap(label);
      if (detail)
        m2.put(label, m3);
      else {
        var m4 = map();
        String[] keys = { "id", "ipv4" };
        for (var k : keys)
          m4.putUnsafe(k, m3.getUnsafe(k));
        m2.put(label, m4);
      }
    }
    pr(m2);
  }

  private void deleteLinode() {
    var a = cmdLineArgs();

    var label = a.nextArg("");
    if (label.isEmpty())
      throw setError("please specify a label for the linode");
    int id = getLinodeId(label);
    if (id == 0)
      throw setError("linode with label", label, "does not exist");

    callLinode("DELETE", "instances/" + id);

    verifyOk();
    discardIdMap();
  }

  private void callLinode(String action, String endpoint) {
    callLinode(action, endpoint, null);
  }

  private void callLinode(String action, String endpoint, JSMap m) {

    mErrors = null;
    mSysCallOutput = null;

    var sc = new SystemCall();
    sc.setVerbose(verbose());

    sc.arg("curl", //
        "-s", // suppress progress bar
        "-H", "Content-Type: application/json", //
        "-H", "Authorization: Bearer " + config().accessToken());
    sc.arg("-X", action);
    if (m != null)
      sc.arg("-d", m);

    sc.arg("https://api.linode.com/v4/linode/" + endpoint);

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

  private boolean verifyOk() {
    var errors = mErrors;
    if (!errors.isEmpty())
      setError("Errors in system call!", INDENT, errors);
    return true;
  }

  private JSMap output() {
    return mSysCallOutput;
  }

  private int getLinodeId(String label) {
    var m = getLinode(label);
    if (m == null)
      return 0;
    return m.getInt("id");
  }

  private JSMap getLinode(String label) {
    return labelToIdMap().optJSMap(label);
  }

  private void discardIdMap() {
    mLinodeMap = null;
  }

  private JSMap labelToIdMap() {
    if (mLinodeMap == null) {
      var m = map();
      callLinode("GET", "instances");
      verifyOk();

      var nodes = output().getList("data");
      for (var m2 : nodes.asMaps()) {
        m.put(m2.get("label"), m2);
      }
      mLinodeMap = m;
    }
    return mLinodeMap;
  }

  private JSMap mLinodeMap;
  private LinodeConfig mConfig;
  private JSList mErrors;
  private JSMap mSysCallOutput;

}
