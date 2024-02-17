package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.gen.LinodeConfig;
import dev.gen.LinodeEntry;
import js.app.CmdLineArgs;
import js.base.BaseObject;
import js.base.DateTimeTools;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.webtools.gen.RemoteEntityInfo;

public class LinodeHandler extends BaseObject implements RemoteHandler {

  @Override
  protected String supplyName() {
    return "linode";
  }

  @Override
  public void create(CmdLineArgs a, String label) {
    var gpu = a.nextArgIf("gpu");
    if (gpu)
      todo("support gpu");

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

  @Override
  public JSMap listEntities() {
    return listEntities(true);
  }

  @Override
  public JSMap listEntitiesDetailed() {
    return listEntities(true);
  }

  private JSMap listEntities(boolean detailed) {
    var m2 = map();
    var m = labelToIdMap();
    for (var m3 : m.values()) {
      var label = m3.label();
      m2.put(label, displayLinodeInfo(m3, detailed));
    }
    return m2;
  }

  private JSMap displayLinodeInfo(LinodeEntry m3, boolean detail) {
    if (detail)
      return m3.toJson();
    return m3.toJson().remove("linode_info");
  }

  @Override
  public void delete(String label) {
    int id = getLinodeId(label, true);
    callLinode("DELETE", "instances/" + id);
    verifyOk();
    discardLinodeInfo();
  }

  @Override
  public RemoteEntityInfo select(String label) {
    var ent = getLinodeInfo(label, true);
    waitUntilRunning(ent);
    createSSHScript(ent);

    var b = RemoteEntityInfo.newBuilder();
    b.id(label) //
        .url(ent.ipAddr()) //
    ;
    return b;
  }

  private LinodeConfig config() {
    if (mConfig == null) {
      var c = LinodeConfig.DEFAULT_INSTANCE;
      File secrets = new File("linode_secrets.json");
      log("looking for secrets file #1:", INDENT, Files.infoMap(secrets));
      if (!secrets.exists()) {
        secrets = new File(Files.homeDirectory(), ".ssh/linode_secrets.json");
        log("looking for secrets file #2:", INDENT, Files.infoMap(secrets));
      }
      log("looking for secrets in:", secrets, "exists:", secrets.exists());
      if (secrets.exists()) {
        c = Files.parseAbstractData(LinodeConfig.DEFAULT_INSTANCE, secrets);
      }
      if (c.accessToken().isEmpty())
        badArg("No linode access_token provided");

      mConfig = c.build();
      log("config:", INDENT, mConfig);
    }
    return mConfig;
  }

  private void waitUntilRunning(LinodeEntry ent) {
    long startTime = 0;
    while (true) {
      var stat = ent.linodeInfo().opt("status", "");
      if (stat.isEmpty())
        badState("no status found for entry:", INDENT, ent);
      if (stat.equals("running"))
        break;
      // Discard cached linode info, to force update
      discardLinodeInfo();
      long curr = System.currentTimeMillis();
      if (startTime == 0)
        startTime = curr;
      if (curr - startTime > DateTimeTools.SECONDS(30))
        badState("timed out waiting for status = 'running'", INDENT, ent);
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
    sb.append(" -oStrictHostKeyChecking=no");
    sb.append(" $@");
    sb.append('\n');
    File f = new File(Files.homeDirectory(), "bin/sshe");
    var fl = Files.S;
    fl.writeString(f, sb.toString());
    fl.chmod(f, "u+x");
    todo("use some utility function for this");
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
      badState("Errors in system call!", INDENT, errors);
    return true;
  }

  private JSMap output() {
    return mSysCallOutput;
  }

  private LinodeEntry getLinodeInfo(String label, boolean mustExist) {
    var m = getLinode(label);
    if (m == null) {
      if (mustExist)
        badArg("label not found:", label);
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
