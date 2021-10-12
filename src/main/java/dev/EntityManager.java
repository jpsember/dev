package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.gen.RemoteEntityInfo;
import dev.gen.RemoteEntityCollection;
import js.base.BaseObject;
import js.file.Files;

public class EntityManager extends BaseObject {

  public static EntityManager sharedInstance() {
    if (sSharedInstance == null) {
      loadTools();
      sSharedInstance = new EntityManager();
    }
    return sSharedInstance;
  }

  private static EntityManager sSharedInstance;

  public RemoteEntityCollection entities() {
    if (mEntities == null) {
      mEntities = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE, entityFile());
      mMutable = null;
    }
    return mEntities;
  }

  public RemoteEntityInfo optionalActiveEntity() {
    return entryFor(entities().activeEntity());
  }

  /**
   * Get RemoteEntryInfo for a particular key. Return default entity if key is
   * null or empty
   */
  public RemoteEntityInfo entryFor(String key) {
    RemoteEntityInfo ent = optionalEntryFor(key);
    if (!nullOrEmpty(key) && ent == RemoteEntityInfo.DEFAULT_INSTANCE)
      throw badState("No entity found for key:", key, INDENT, entities());
    return ent;
  }

  public RemoteEntityInfo optionalEntryFor(String key) {
    RemoteEntityInfo ent = RemoteEntityInfo.DEFAULT_INSTANCE;
    if (!nullOrEmpty(key)) {
      RemoteEntityInfo ent2 = entities().entityMap().get(key);
      if (ent2 != null)
        ent = ent2;
    }
    return ent;
  }

  public RemoteEntityInfo activeEntity() {
    RemoteEntityInfo ent = optionalActiveEntity();
    if (ent == RemoteEntityInfo.DEFAULT_INSTANCE)
      throw badState("No active remote entity:", INDENT, entities());
    return ent;
  }

  public void setActive(String tag) {
    if (!entities().entityMap().containsKey(tag))
      throw badArg("entity not found:", tag);
    mutable().activeEntity(tag);
    flushChanges();
  }

  /**
   * Store updated version of entity
   */
  public void updateEnt(RemoteEntityInfo entity) {
    log("updateEnt:", entity.id());
    RemoteEntityInfo prev = entities().entityMap().get(entity.id());
    if (!entity.equals(prev)) {
      Map<String, RemoteEntityInfo> m = new HashMap<>(mutable().entityMap());
      m.put(entity.id(), entity);
      mutable().entityMap(m);
      log("...storing modified entity:", INDENT, entity);
      flushChanges();
    } else
      log("...no changes");
  }

  private File entityFile() {
    if (mEntityFile == null) {
      mEntityFile = new File(Files.S.projectConfigDirectory(), "entity_map.json");
    }
    return mEntityFile;
  }

  private RemoteEntityCollection.Builder mutable() {
    if (mMutable == null) {
      mMutable = entities().toBuilder();
    }
    return mMutable;
  }

  private void flushChanges() {
    if (mMutable == null)
      return;
    RemoteEntityCollection updated = mMutable.build();
    if (!updated.equals(mEntities)) {
      log("...flushing changes to:", entityFile());
      mEntities = mMutable.build();
      Files.S.writePretty(entityFile(), mEntities);
    }
  }

  private RemoteEntityCollection mEntities;
  private File mEntityFile;
  private RemoteEntityCollection.Builder mMutable;
}
