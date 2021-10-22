package dev;

import static js.base.Tools.*;

import java.io.File;

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

  public static File determineProjectDir(RemoteEntityInfo ent) {
    String s = ent.projectDir().toString();
    String marker = "/" + ent.user() + "/";
    int j = s.indexOf(marker);
    checkState(j >= 0, "can't find substring:", marker, "within:", s);
    String homeDir = s.substring(0, j + marker.length());
    return new File(homeDir);
  }

  private static RemoteEntityInfo sRemoteEntityInfo;

}
