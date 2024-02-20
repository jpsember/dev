package dev;

import static js.base.Tools.*;

import java.util.Map;

import dev.gen.RemoteConfig;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.BaseObject;
import js.base.BasePrinter;
import js.webtools.RemoteManager;

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
  protected void getOperSpecificHelp(BasePrinter b) {
    b.pr("list                   -- list entities");
    b.pr("details                -- list entities with full detail");
    b.pr("select <label>         -- select entity to be 'active'");
    b.pr("create <label>\n" + "   [image <imglabel>]  -- create entity");
    b.pr("delete <label>         -- delete entity");
    b.pr("createimage <imglabel> -- create image from current entity");
    b.pr("images                 -- list images");
  }

  @Override
  public void perform() {
    var mgr = RemoteManager.SHARED_INSTANCE;
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
        var entityLabel = parseLabel(a);
        String imageLabel = null;
        if (a.nextArgIf("image")) {
          imageLabel = parseLabel(a);
        }

        if (handler().listEntities().containsKey(entityLabel))
          setError("entity already exists:", entityLabel);
        handler().create(a, entityLabel, imageLabel);
        handler().select(entityLabel);
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
        if (mgr.info().activeEntity() != null && mgr.info().activeEntity().label().equals(label))
          mgr.infoEdit().activeEntity(null);
        mgr.flush();
      }
        break;
      case "select": {
        var label = parseLabel(a);
        var info = handler().select(label);
        mgr.infoEdit().activeEntity(info);
        mgr.flush();
      }
        break;
      case "createimage": {
        var imageLabel = parseLabel(a);
        handler().createImage(imageLabel);
      }
        break;
      case "images":
        pr(handler().getImagesList());
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
    var mgr = RemoteManager.SHARED_INSTANCE;
    mgr.infoEdit().activeHandlerName(name);
    mgr.flush();
  }

  private RemoteHandler handler() {
    var mgr = RemoteManager.SHARED_INSTANCE;
    var name = mgr.info().activeHandlerName();
    if (name.isEmpty())
      setError("no active remote handler defined");
    var h = sHandlerMap.get(name);
    if (h == null)
      setError("no handler found for name:", name);
    if (verbose() && h instanceof BaseObject) {
      ((BaseObject) h).setVerbose();
    }
    return h;
  }

  private RemoteConfig mConfig;
  private static Map<String, RemoteHandler> sHandlerMap = hashMap();

  public static void registerHandler(RemoteHandler handler) {
    sHandlerMap.put(handler.name(), handler);
  }

  static {
    RemoteOper.registerHandler(new LinodeHandler());
  }

}
