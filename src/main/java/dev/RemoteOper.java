package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.Map;

import dev.gen.RemoteConfig;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;
import js.webtools.gen.RemoteInfo;

public class RemoteOper extends AppOper {

  @Override
  public String userCommand() {
    return "remote";
  }

  @Override
  public String getHelpDescription() {
    return "manage remote entities, e.g. linode or AWS";
  }

  @Override
  public RemoteConfig defaultArgs() {
    return RemoteConfig.DEFAULT_INSTANCE;
  }

  @Override
  public RemoteConfig config() {
    if (mConfig == null) {
      mConfig = (RemoteConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var a = cmdLineArgs();
    while (a.hasNextArg()) {
      var cmd = a.nextArg();

      var c = sHandlerMap.get(cmd);
      if (c != null) {
        setHandler(cmd);
        continue;
      }

      switch (cmd) {
      default:
        throw setError("Unrecognized command:", cmd);
      case "create": {
        var label = parseLabel(a);
        if (handler().listEntities().containsKey(label))
          setError("entity already exists:", label);
        handler().create(a, label);
        handler().select(label);
      }
        break;
      case "list":
        pr(handler().listEntities());
        break;
      case "details":
        pr(handler().listEntitiesDetailed());
        break;
      case "delete": {
        var label = parseLabel(a);
        handler().delete(label);
        if (remoteInfo().activeEntity() != null && remoteInfo().activeEntity().id().equals(label))
          edit().activeEntity(null);
        flush();
      }
        break;
      case "select": {
        var label = parseLabel(a);
        var info = handler().select(label);
        edit().activeEntity(info);
        flush();
      }
        break;
      }
    }
  }

  private String parseLabel(CmdLineArgs a) {
    var label = a.nextArg("");
    if (label.isEmpty())
      setError("please provide a label");
    return label;
  }

  private void setHandler(String name) {
    var h = sHandlerMap.get(name);
    if (h == null)
      setError("no registered handler:", name);
    edit().activeHandlerName(name);
    flush();
  }

  private RemoteHandler handler() {
    var name = remoteInfo().activeHandlerName();
    if (name.isEmpty())
      setError("no active remote handler defined");
    var h = sHandlerMap.get(name);
    if (h == null)
      setError("no handler found for name:", name);
    return h;
  }

  private RemoteInfo.Builder remoteInfo() {
    if (mRemoteInfo == null) {
      mRemoteInfo = Files.parseAbstractDataOpt(RemoteInfo.DEFAULT_INSTANCE, remoteInfoPersistFile())
          .toBuilder();
      mRmod = false;
    }
    return mRemoteInfo;
  }

  private RemoteInfo.Builder edit() {
    var b = remoteInfo();
    mRmod = true;
    return b;
  }

  private File remoteInfoPersistFile() {
    return new File(Files.homeDirectory(), ".remote_info");
  }

  private void flush() {
    if (!mRmod)
      return;
    files().write(remoteInfoPersistFile(), remoteInfo());
    mRmod = false;
  }

  private RemoteInfo.Builder mRemoteInfo;
  private boolean mRmod;
  private RemoteConfig mConfig;
  private static Map<String, RemoteHandler> sHandlerMap = hashMap();

  public static void registerHandler(RemoteHandler handler) {
    sHandlerMap.put(handler.name(), handler);
  }

}
