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
    if (!nullOrEmpty(key) && ent == null)
      throw badState("No entity found for key:", key, INDENT, entities());
    return ent;
  }

  public RemoteEntityInfo optionalEntryFor(String key) {
    RemoteEntityInfo ent = null;
    if (!nullOrEmpty(key))
      ent = entities().entityMap().get(key);
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
    RemoteEntityInfo template = staticEntities().entityTemplate();
    RemoteEntityInfo original = null;
    if (verbose())
      original = builder.build();
    builder.id(id);
    if (builder.osType() == OsType.UNKNOWN)
      builder.osType(template.osType());
    if (builder.user().isEmpty())
      builder.user(template.user());
    if (Files.empty(builder.projectDir()))
      builder.projectDir(template.projectDir());
    if (verbose()) {
      RemoteEntityInfo modified = builder.build();
      if (!modified.equals(original))
        log("applied defaults, result:", INDENT, modified);
    }
    return builder;
  }

  private static final String ENTITIES_FILENAME_DYNAMIC = "entity_map.json";
  private static final String ENTITIES_FILENAME_STATIC = "entity_map_static.json";

  private File dynamicEntityFile() {
    return new File(Files.S.projectConfigDirectory(), ENTITIES_FILENAME_DYNAMIC);
  }

  private File staticEntityFile() {
    return new File(Files.S.projectConfigDirectory(), ENTITIES_FILENAME_STATIC);
  }

  private RemoteEntityCollection.Builder mutableStatic() {
    mutable();
    return mMutableEntitiesStatic;
  }

  private RemoteEntityCollection.Builder mutable() {
    todo("have a separate private method to read the static and dynamic, to avoid confusing with lazy");
    if (mMutableEntities == null) {
      checkState(!mNestedCallFlag);
      mNestedCallFlag = true;

      readDynamicEntries();
      readStaticEntries();

      fixMissingDynamicEntries();
      updateDynamicEntities();

      // Flush any changes immediately, in case client isn't going to make any changes
      flushChanges();
      mNestedCallFlag = false;
    }
    return mMutableEntities;
  }

  private void readDynamicEntries() {
    mOriginalEntitiesDynamic = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        dynamicEntityFile());
    mMutableEntities = mOriginalEntitiesDynamic.toBuilder();
  }

  private void readStaticEntries() {
    mOriginalEntitiesStatic = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        staticEntityFile());
    mMutableEntitiesStatic = mOriginalEntitiesStatic.toBuilder();
  }

  /**
   * Copy any entities that appear in the static list that are missing from the
   * dynamic list
   */
  private void fixMissingDynamicEntries() {
    Map<String, RemoteEntityInfo> modified = hashMap();
    modified.putAll(mMutableEntities.entityMap());
    for (Map.Entry<String, RemoteEntityInfo> entry : mMutableEntitiesStatic.entityMap().entrySet()) {
      if (!mMutableEntities.entityMap().containsKey(entry.getKey()))
        modified.put(entry.getKey(), entry.getValue());
    }
    mMutableEntities.entityMap(modified);
  }

  /**
   * process all dynamic entities, applying defaults in case any are missing
   */
  private void updateDynamicEntities() {
    Map<String, RemoteEntityInfo> modified = hashMap();
    for (Map.Entry<String, RemoteEntityInfo> entry : mMutableEntities.entityMap().entrySet()) {
      RemoteEntityInfo filtered = applyDefaults(entry.getKey(), entry.getValue());
      modified.put(filtered.id(), filtered.build());
    }
    mMutableEntities.entityMap(modified);
  }

  private boolean mNestedCallFlag;

  private RemoteEntityCollection staticEntities() {
    mutable();
    return mMutableEntitiesStatic;
  }

  private void flushChanges() {
    RemoteEntityCollection updated;

    updated = mutable().build();

    if (!updated.equals(mOriginalEntitiesDynamic)) {
      File file = dynamicEntityFile();
      log("...flushing (dynamic) changes to:", file);
      mOriginalEntitiesDynamic = mutable().build();
      Files.S.writePretty(file, mOriginalEntitiesDynamic);
    }

    updateStaticEntityList();

    updated = mutableStatic().build();
    if (!updated.equals(mOriginalEntitiesStatic)) {
      File file = staticEntityFile();
      log("...flushing (static) changes to:", file);
      mOriginalEntitiesStatic = updated;
      Files.S.writePretty(file, updated);
    }
  }

  // Copy all the dynamic entities to the static map, after removing dynamic fields
  private void updateStaticEntityList() {
    Map<String, RemoteEntityInfo> modified;
    modified = hashMap();
    for (RemoteEntityInfo ent : mMutableEntities.entityMap().values()) {
      modified.put(ent.id(), ent.toBuilder()//
          .port(0) //
          .url(null) //
          .build());
    }
    mutableStatic().entityMap(modified);
  }

  private RemoteEntityCollection mOriginalEntitiesDynamic;
  private RemoteEntityCollection mOriginalEntitiesStatic;
  private RemoteEntityCollection.Builder mMutableEntities;
  private RemoteEntityCollection.Builder mMutableEntitiesStatic;

}
