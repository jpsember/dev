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

  /**
   * <pre>
   * 
   * Notes specific to OSX configuration
   * -----------------------------------
   * 
   * + disabling strange messages in Terminal when ending a session:
   *    Create this file in home directory (https://stackoverflow.com/questions/32418438):
   *       touch ~/.bash_sessions_disable
   * 
   *    Note, this disables more functionality related to bash sessions introduced in El Capitan.
   * 
   * 
   */
}
