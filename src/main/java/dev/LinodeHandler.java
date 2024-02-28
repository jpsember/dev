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
import js.webtools.RemoteManager;
import js.webtools.gen.RemoteEntityInfo;

public class LinodeHandler extends BaseObject implements RemoteHandler {

  @Override
  protected String supplyName() {
    return "linode";
  }

  @Override
  public void create(CmdLineArgs a, String entityLabel, String imageLabel) {
    var gpu = a.nextArgIf("gpu");
    if (gpu)
      todo("support gpu");

    var m = map();
    m //
        .put("authorized_keys", JSList.with(config().authorizedKeys())) //
        .put("image", "linode/ubuntu20.04") //
        .put("label", entityLabel) //
        .put("region", "us-sea")//
        .put("root_pass", config().rootPassword()) //
        .put("type", "g6-nanode-1") //
    ;
    if (nonEmpty(imageLabel))
      m.put("image", imageLabel);

    // https://www.linode.com/docs/api/linode-instances/#linode-create
    callLinode("POST", "linode/instances", m);
    verifyOk();
    discardLinodeInfo();
  }

  @Override
  public JSMap listEntities() {
    return listEntities(false);
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
    callLinode("DELETE", "linode/instances/" + id);
    verifyOk();
    discardLinodeInfo();
  }

  @Override
  public RemoteEntityInfo select(String label) {
    var ent = getLinodeInfo(label, true);
    waitUntilRunning(ent);
    createSSHScript(ent);

    var b = RemoteEntityInfo.newBuilder();
    b.label(label) //
        .url(ent.ipAddr()) //
        .user("root") //
        .projectDir(new File("/root"));
    ;
    return b;
  }

  @Override
  public void createImage(String imageLabel) {
    var v = RemoteManager.SHARED_INSTANCE;
    var ent = v.activeEntity();
    var id = getLinodeId(ent.label(), true);

    // We need to get the disk id of the linode entity.
    //
    int diskId = 0;
    {
      var getDisksOutput = callLinode("GET", "linode/instances/" + id + "/disks");
      var disks = getDisksOutput.getList("data");
      JSMap diskMap = null;
      for (var mc : disks.asMaps()) {
        if (mc.get("filesystem").equals("swap"))
          continue;
        if (diskMap != null)
          badState("multiple disks returned:", INDENT, getDisksOutput);
        diskMap = mc;
      }
      checkState(diskMap != null, "no disks found:", INDENT, getDisksOutput);
      diskId = diskMap.getInt("id");
    }

    var m = map();
    m.put("disk_id", diskId).put("label", imageLabel).put("description", IMAGE_DESCRIPTION);
    // https://www.linode.com/docs/api/images/#image-create
    callLinode("POST", "images", m);

    waitUntilImageAvailable(imageLabel);
  }

  private static final String IMAGE_DESCRIPTION = "created by dev 'remote' command";

  @Override
  public JSList getImagesList() {
    var lst = callLinode("GET", "images").getList("data");
    // Suppress all images not created by this tool
    var out = list();
    for (var m : lst.asMaps()) {
      if (m.opt("description", "").equals(IMAGE_DESCRIPTION))
        out.add(m);
    }
    return out;
  }

  private void waitUntilImageAvailable(String imageLabel) {
    long startTime = 0;
    long delay = 0;

    while (true) {
      if (startTime != 0)
        DateTimeTools.sleepForRealMs(5000);
      long curr = System.currentTimeMillis();
      if (startTime == 0)
        startTime = curr;
      delay = curr - startTime;
      if (delay > DateTimeTools.SECONDS(600))
        badState("timed out waiting for status = 'available'", INDENT, imageLabel);
      pr("...delay waiting for status = 'available':", (delay / 1000) + "s");

      // https://www.linode.com/docs/api/images/#images-list
      var m = callLinode("GET", "images");
      JSMap targetMap = null;
      for (var m2 : m.getList("data").asMaps()) {
        if (m2.get("label").equals(imageLabel))
          targetMap = m2;
      }
      pr("target map:", INDENT, targetMap);
      if (targetMap != null && targetMap.get("status").equals("available"))
        break;
    }
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

  private void waitUntilRunning(LinodeEntry ent2) {
    long startTime = 0;
    long delay = 0;
    var label = ent2.label();

    while (true) {
      if (startTime != 0)
        DateTimeTools.sleepForRealMs(5000);
      long curr = System.currentTimeMillis();
      if (startTime == 0)
        startTime = curr;
      delay = curr - startTime;
      if (delay > DateTimeTools.SECONDS(600))
        badState("timed out waiting for status = 'running'", INDENT, label);
      pr("...delay waiting for status = 'running':", (delay / 1000) + "s");
      // Discard cached linode info, to force update
      discardLinodeInfo();
      var ent = getLinode(label);
      var stat = ent.linodeInfo().opt("status", "");
      if (stat.isEmpty())
        badState("no status found for entry:", INDENT, ent);
      if (stat.equals("running"))
        break;
    }
  }

  private void createSSHScript(LinodeEntry ent) {
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
    File f = new File(Files.binDirectory(), "sshe");
    var fl = Files.S;
    fl.writeString(f, sb.toString());
    fl.chmod(f, 755);
  }

  private JSMap callLinode(String action, String endpoint) {
    return callLinode(action, endpoint, null);
  }

  private JSMap callLinode(String action, String endpoint, JSMap m) {

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

    sc.arg("https://api.linode.com/v4/" + endpoint);

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
    return output();
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
      var mout = callLinode("GET", "linode/instances");
      verifyOk();
      var mp = new HashMap<String, LinodeEntry>();
      var nodes = mout.getList("data");
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

}
