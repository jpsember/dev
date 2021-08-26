package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.RemoteEntityMap;
import js.file.Files;

public class EntityManager {

  public static EntityManager sharedInstance() {
    if (sSharedInstance == null) {
      loadTools();
      sSharedInstance = new EntityManager();
    }
    return sSharedInstance;
  }

  private static EntityManager sSharedInstance;

  public RemoteEntityMap entityMap() {
    if (mEntityMap == null) {
      File config = Utils.getFileWithinParents(null, "project_config");
      File entityFile = new File(config, "entity_map.json");
      mEntityMap = Files.parseAbstractData(RemoteEntityMap.DEFAULT_INSTANCE, entityFile);
    }
    return mEntityMap;
  }

  private RemoteEntityMap mEntityMap;

}
