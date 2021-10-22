package dev;

import static js.base.Tools.*;

import dev.gen.RemoteEntityInfo;
import js.file.Files;

public final class Utils {

  public static RemoteEntityInfo ourEntityInfo() {
    if (sRemoteEntityInfo == null) {
      loadTools();
      sRemoteEntityInfo = RemoteEntityInfo.DEFAULT_INSTANCE.parse(Files.S.entityInfo());
    }
    return sRemoteEntityInfo;
  }

  private static RemoteEntityInfo sRemoteEntityInfo;

}
