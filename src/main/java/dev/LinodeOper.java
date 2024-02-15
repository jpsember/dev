package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.gen.LinodeConfig;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

public class LinodeOper extends AppOper {

  public static final String TEST_ARGS = "--exceptions -v linode " //
      //+ "create label cpu2";
      + "list";
      //+ "delete label cpu2";

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

    var label = a.nextArgIf("label", "");
    var gpu = a.nextArgIf("gpu");
    if (gpu)
      todo("support gpu");

    if (label.isEmpty())
      throw setError("please specify a label for the linode");
    if (getLinodeId(label) != null)
      throw setError("linode with label", label, "already exists");

    log("config:", INDENT, config());

    var m = map();
    m.put("authorized_users", list().add("jpsember")) //
        .put("authorized_keys", JSList.with(config().authorizedKeys())) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", label) //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;

    callLinode("instances", m);
    verifyOk();
    discardIdMap();
  }

  private void listNodes() {
    callLinode("instances");
    verifyOk();

    pr(labelToIdMap());
    
    var m2 = map();
    var nodes = output().getList("data");
    for (var m : nodes.asMaps()) {
      m2.put(m.get("label"), m);
    }
    pr(m2);

    //    pr(output());
  }

  private Integer getLinodeId(String label) {
    return labelToIdMap().get(label);
  }

  private Map<String, Integer> labelToIdMap() {
    if (mLabelToIdMap == null) {
      var m = new HashMap<String, Integer>();

      callLinode("instances");
      verifyOk();

      var nodes = output().getList("data");
      for (var m2 : nodes.asMaps()) {
        var id = m2.getInt("id");
        m.put(m2.get("label"), id);
      }
      mLabelToIdMap = m;
    }
    return mLabelToIdMap;
  }

  private Map<String, Integer> mLabelToIdMap;

  private void discardIdMap() {
    mLabelToIdMap = null;
  }

  private void deleteLinode() {
    var a = cmdLineArgs();

    var label = a.nextArgIf("label", "");
    if (label.isEmpty())
      throw setError("please specify a label for the linode");
    if (getLinodeId(label) == null)
      throw setError("linode with label", label, "does not exist");

    var m = map();
    m.put("authorized_users", list().add("jpsember")) //
        .put("authorized_keys", JSList.with(config().authorizedKeys())) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", label) //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;

    callLinode("instances", m);
    verifyOk();
    discardIdMap();
  }

  private void callLinode(String endpoint) {
    callLinode(endpoint, null);
  }

  private void callLinode(String endpoint, JSMap m) {

    mErrors = null;
    mSysCallOutput = null;
    mSysCall = m;

    var sc = new SystemCall();
    sc.setVerbose(verbose());

    sc.arg("curl", //
        "-s", // suppress progress bar
        "-H", "Content-Type: application/json", //
        "-H", "Authorization: Bearer " + config().accessToken());
    if (m != null) {
      sc.arg("-X", "POST", "-d", m //
      );
    } else {
      sc.arg("-X", "GET");
    }
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

  private LinodeConfig mConfig;
  private JSList mErrors;
  private JSMap mSysCallOutput;
  private JSMap mSysCall;

}
