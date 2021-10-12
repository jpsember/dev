package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.gen.RemoteEntityInfo;
import dev.gen.OsType;
import dev.gen.RemoteEntityCollection;
import js.base.BaseObject;
import js.file.Files;

public class EntityManager extends BaseObject {

  public static EntityManager sharedInstance() {
    if (sSharedInstance == null)
      sSharedInstance = new EntityManager();
    return sSharedInstance;
  }

  private static EntityManager sSharedInstance;

  public RemoteEntityCollection entities() {
    return mutable();
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
    checkArgument(!entity.id().isEmpty(), "missing id:", INDENT, entity);
    entity = applyDefaults(entity.id(), entity.toBuilder()).build();
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

  private RemoteEntityInfo.Builder applyDefaults(String id, RemoteEntityInfo entity) {
    RemoteEntityInfo.Builder builder = entity.toBuilder();
    RemoteEntityInfo template = entities().entityTemplate();
    builder.id(id);
    if (builder.osType() == OsType.UNKNOWN)
      builder.osType(template.osType());
    if (builder.user().isEmpty())
      builder.user(template.user());
    if (Files.empty(builder.projectDir()))
      builder.projectDir(template.projectDir());
    return builder;
  }

  private File entityFile() {
    if (mEntityFile == null) {
      mEntityFile = new File(Files.S.projectConfigDirectory(), "entity_map.json");
    }
    return mEntityFile;
  }

  private RemoteEntityCollection.Builder mutable() {
    if (mMutableEntities == null) {
      mOriginalEntities = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE, entityFile())
          .toBuilder();
      mMutableEntities = mOriginalEntities.toBuilder();

      // process all entities, applying defaults in case any are missing
      //
      Map<String, RemoteEntityInfo> modified = hashMap();
      for (Map.Entry<String, RemoteEntityInfo> entry : mMutableEntities.entityMap().entrySet()) {
        RemoteEntityInfo filtered = applyDefaults(entry.getKey(), entry.getValue());
        modified.put(filtered.id(), filtered);
      }
      mMutableEntities.entityMap(modified);
    }
    return mMutableEntities;
  }

  private void flushChanges() {
    if (mMutableEntities == null)
      return;
    RemoteEntityCollection updated = mMutableEntities.build();
    if (!updated.equals(mOriginalEntities)) {
      log("...flushing changes to:", entityFile());
      mOriginalEntities = mMutableEntities.build();
      Files.S.writePretty(entityFile(), mOriginalEntities);
    }
  }

  private RemoteEntityCollection mOriginalEntities;
  private File mEntityFile;
  private RemoteEntityCollection.Builder mMutableEntities;
}
