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
  private File mEntityFile;

  public RemoteEntityMap entityMap() {
    if (mEntityMap == null) {
      mEntityMap = Files.parseAbstractData(RemoteEntityMap.DEFAULT_INSTANCE, entityFile());
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
      throw badState("No active remote entity:", INDENT, entityMap());
    return ent;
  }

  public void setActive(String tag) {
    if (!entityMap().entityMap().containsKey(tag))
      throw badArg("entity not found in map:", tag);

    RemoteEntityMap.Builder b = entityMap().toBuilder();
    b.activeEntity(tag);
    mEntityMap = b.build();

    Files.S.writePretty(entityFile(), mEntityMap);
  }

  private File entityFile() {
    if (mEntityFile == null) {
      mEntityFile = new File(Files.S.projectConfigDirectory(), "entity_map.json");
    }
    return mEntityFile;
  }

  private RemoteEntityMap mEntityMap;

}
