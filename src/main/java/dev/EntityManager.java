package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.RemoteEntityInfo;
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
      File entityFile = new File(Files.S.projectConfigDirectory(), "entity_map.json");
      mEntityMap = Files.parseAbstractData(RemoteEntityMap.DEFAULT_INSTANCE, entityFile);
    }
    return mEntityMap;
  }

  public RemoteEntityInfo optionalActiveEntity() {
    RemoteEntityInfo ent = RemoteEntityInfo.DEFAULT_INSTANCE;
    String key = entityMap().activeEntity();
    if (!key.isEmpty()) {
      ent = entityMap().entityMap().get(key);
      if (ent == null)
        throw badState("No entity found for active entity key:", key, INDENT, entityMap());
    }
    return ent;
  }

  public RemoteEntityInfo activeEntity() {
    RemoteEntityInfo ent = optionalActiveEntity();
    if (ent == RemoteEntityInfo.DEFAULT_INSTANCE)
      throw badState("No active remote entity:",INDENT,entityMap());
    return ent;
  }

  private RemoteEntityMap mEntityMap;

}
