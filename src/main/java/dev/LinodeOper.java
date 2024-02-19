package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.gen.LinodeConfig;
import dev.gen.LinodeEntry;
import js.app.AppOper;
import js.base.BasePrinter;
import js.base.DateTimeTools;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.webtools.EntityManager;

@Deprecated
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
    b.pr("select <label>");
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
      log("config:", INDENT, mConfig);
    }
    return mConfig;
  }

  @Override
  public void perform() {
    todo("add a 'prepare' suboper that rsyncs scripts, and runs one of those scripts remotely");
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
      case "select":
        selectLinode(a.nextArg());
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
    if (getLinodeId(label, false) != 0)
      throw setError("linode with label", label, "already exists");

    var m = map();
    m //
        .put("authorized_keys", JSList.with(config().authorizedKeys())) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", label) //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;

    callLinode("POST", "instances", m);
    verifyOk();
    discardLinodeInfo();
  }

  private void listNodes() {
    var detail = cmdLineArgs().nextArgIf("detail");

    var m2 = map();
    var m = labelToIdMap();
    for (var m3 : m.values()) {

      var label = m3.label();
      m2.put(label, displayLinodeInfo(m3, detail));
      //      if (detail)
      //        m2.put(label, m3.linodeInfo());
      //      else {
      //        m2.put(label, m3.toJson().remove("linode_info"));
      //      }
    }
    pr(m2);
  }

  private JSMap displayLinodeInfo(LinodeEntry m3, boolean detail) {
    if (detail)
      return m3.toJson();
    return m3.toJson().remove("linode_info");
  }

  private void deleteLinode() {
    var a = cmdLineArgs();

    var label = a.nextArg("");
    if (label.isEmpty())
      throw setError("please specify a label for the linode");
    int id = getLinodeId(label, true);

    callLinode("DELETE", "instances/" + id);

    verifyOk();
    discardLinodeInfo();
  }

  private void selectLinode(String label) {
    var ent = getLinodeInfo(label, true);
    waitUntilRunning(ent);
    EntityManager.sharedInstance().setLinodeInfo(displayLinodeInfo(ent, false));
    createSSHScript(ent);
    pr(displayLinodeInfo(ent, false));
  }

  private void waitUntilRunning(LinodeEntry ent) {
    long startTime = 0;
    while (true) {
      var stat = ent.linodeInfo().opt("status", "");
      if (stat.isEmpty())
        setError("no status found for entry:", INDENT, ent);
      if (stat.equals("running"))
        break;
      // Discard cached linode info, to force update
      discardLinodeInfo();
      long curr = System.currentTimeMillis();
      if (startTime == 0)
        startTime = curr;
      if (curr - startTime > DateTimeTools.SECONDS(30))
        setError("timed out waiting for status = 'running'", INDENT, ent);
    }
  }

  private void createSSHScript(LinodeEntry ent) {
    waitUntilRunning(ent);
    StringBuilder sb = new StringBuilder();
    sb.append("#!/usr/bin/env bash\n");
    sb.append("echo \"Connecting to: ");
    sb.append(ent.label());
    sb.append("\"\n");
    sb.append("ssh ");
    sb.append("root");
    sb.append("@");
    sb.append(ent.ipAddr());
    //    sb.append(" -p ");
    //    sb.append(ent.port());
    sb.append(" -oStrictHostKeyChecking=no");
    sb.append(" $@");
    sb.append('\n');
    File f = new File(Files.homeDirectory(), "bin/sshe");
    files().writeString(f, sb.toString());
    files().chmod(f, "u+x");
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

  private LinodeEntry getLinodeInfo(String label, boolean mustExist) {
    var m = getLinode(label);
    if (m == null) {
      if (mustExist)
        throw setError("label not found:", label);
      return null;
    }
    return m;
  }

  private int getLinodeId(String label, boolean mustExist) {
    var m = getLinodeInfo(label, mustExist);
    if (m == null)
      return 0;
    return m.id();
  }

  private LinodeEntry getLinode(String label) {
    return labelToIdMap().get(label);
  }

  private void discardLinodeInfo() {
    mLinodeMap = null;
  }

  private Map<String, LinodeEntry> labelToIdMap() {
    if (mLinodeMap == null) {
      callLinode("GET", "instances");
      verifyOk();

      var mp = new HashMap<String, LinodeEntry>();
      var nodes = output().getList("data");
      for (var m2 : nodes.asMaps()) {
        var ent = LinodeEntry.newBuilder();
        ent.linodeInfo(m2);
        var ipv4 = m2.getList("ipv4");
        if (ipv4.size() != 1)
          badArg("unexpected ipv4:", ipv4, INDENT, m2);
        ent.id(m2.getInt("id")) //
            .label(m2.get("label")) //
            .ipAddr(ipv4.getString(0)) //
        ;
        mp.put(ent.label(), ent.build());
      }
      mLinodeMap = mp;
    }
    return mLinodeMap;
  }

  private Map<String, LinodeEntry> mLinodeMap;
  private LinodeConfig mConfig;
  private JSList mErrors;
  private JSMap mSysCallOutput;

  static {
    RemoteOper.registerHandler(new LinodeHandler());
  }
}
