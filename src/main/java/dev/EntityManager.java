package dev;

import static js.base.Tools.*;

import java.io.File;
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

  // ------------------------------------------------------------------
  // Supplying Files object, to support dryrun operation
  // ------------------------------------------------------------------

  public EntityManager withFiles(Files files) {
    mFiles = files;
    return this;
  }

  private Files files() {
    return mFiles;
  }

  private Files mFiles = Files.S;

  /**
   * Get an immutable copy of the current (dynamic) entities
   */
  public RemoteEntityCollection currentEntities() {
    return dynamicRegister().build();
  }

  private RemoteEntityCollection entities() {
    return dynamicRegister();
  }

  public RemoteEntityInfo optionalActiveEntity() {
    todo(
        "the static list should be the source of truth, e.g. don't create static entries from dynamic ones; and have the dynamic one dotted (hidden) since it is not tracked.");
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
    dynamicRegister().activeEntity(tag);
    flushChanges();
  }

  /**
   * Store updated version of entity
   */
  public RemoteEntityInfo updateEnt(RemoteEntityInfo entity) {

    log("updateEnt:", entity.id());
    checkArgument(!entity.id().isEmpty(), "missing id:", INDENT, entity);

    // Construct template to fetch missing values from.
    // Use the current dynamic version of the entity, or the default template if none yet exists
    //
    RemoteEntityInfo template = dynamicRegister().entityMap().get(entity.id());
    boolean added = (template == null);
    if (added) {
      template = staticRegister().entityTemplate().toBuilder().id(entity.id());
    }

    // Apply defaults from this template
    entity = applyDefaults(entity.id(), entity, template).build();

    // If hidden entity already exists and is not changed, we don't need to continue
    //
    RemoteEntityInfo prevOrNull = dynamicRegister().entityMap().get(entity.id());

    if (entity.equals(prevOrNull)) {
      log("...no changes");
    } else {
      log("...storing", added ? "new" : "modified", "entity:", INDENT, entity);
      dynamicRegister().entityMap().put(entity.id(), entity);
      // Store appropriate version within static register
      RemoteEntityInfo staticVersion = clearDynamicFields(entity).build();
      log("...storing static version:", INDENT, staticVersion);
      staticRegister().entityMap().put(entity.id(), staticVersion);
      // Flush changes to both static and dynamic registers
      flushChanges();
    }
    return entity;
  }

  /**
   * Clear those fields of an entity that are associated with the hidden
   * register: port, url
   */
  private RemoteEntityInfo.Builder clearDynamicFields(RemoteEntityInfo entity) {
    return entity.toBuilder().port(0).url(null);
  }

  private RemoteEntityInfo.Builder applyDefaults(String id, RemoteEntityInfo entity,
      RemoteEntityInfo template) {
    RemoteEntityInfo.Builder builder = entity.toBuilder();
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
    todo("this logging should be moved to where the context makes more sense");
    if (verbose()) {
      RemoteEntityInfo modified = builder.build();
      if (!modified.equals(original))
        log("applied defaults, original:", INDENT, original, OUTDENT, "result:", INDENT, modified);
    }
    return builder;
  }

  private static final String ENTITIES_FILENAME_DYNAMIC = "entity_map.json";
  private static final String ENTITIES_FILENAME_STATIC = "entity_map_static.json";

  private File dynamicEntityFile() {
    return new File(files().projectConfigDirectory(), ENTITIES_FILENAME_DYNAMIC);
  }

  private File staticEntityFile() {
    return new File(files().projectConfigDirectory(), ENTITIES_FILENAME_STATIC);
  }

  private RemoteEntityCollection.Builder staticRegister() {
    dynamicRegister();
    return mStaticEntities;
  }

  private RemoteEntityCollection.Builder dynamicRegister() {
    if (mDynamicEntities == null) {
      checkState(!mNestedCallFlag);
      mNestedCallFlag = true;

      // We must read both registers before attempting any edits, so that the
      // instance fields are initialized
      //
      readRegister();
      readHiddenRegister();

      fixEntries();
      fixHiddenEntries();

      // Flush any changes immediately, in case client isn't going to make any changes
      flushChanges();
      mNestedCallFlag = false;
    }
    return mDynamicEntities;
  }

  private void readHiddenRegister() {
    mOriginalEntitiesDynamic = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        dynamicEntityFile());
    mDynamicEntities = mOriginalEntitiesDynamic.toBuilder();
  }

  private void readRegister() {
    mOriginalEntitiesStatic = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        staticEntityFile());
    mStaticEntities = mOriginalEntitiesStatic.toBuilder();
  }

  /**
   * Process the static entries, applying any fixes; e.g. mismatched id, missing
   * default values
   * 
   */
  private void fixEntries() {
    Map<String, RemoteEntityInfo> updatedEntities = hashMap();
    for (Map.Entry<String, RemoteEntityInfo> entry : mDynamicEntities.entityMap().entrySet()) {
      String id = entry.getKey();
      RemoteEntityInfo original = entry.getValue();
      RemoteEntityInfo fixed = applyDefaults(id, original, mStaticEntities.entityTemplate()).build();
      if (verbose() && !original.equals(fixed))
        log("Fixed entity:", id, INDENT, fixed);
      updatedEntities.put(id, fixed);
    }
    mStaticEntities.entityMap(updatedEntities);
  }

  /**
   * Create any entries that are missing from the hidden registery; and update
   * all hidden entries to agree with their static counterparts
   */
  private void fixHiddenEntries() {
    todo("fix up the static entities first, so e.g. ids match");
    Map<String, RemoteEntityInfo> updatedHiddenEntities = hashMap();
    for (Map.Entry<String, RemoteEntityInfo> entry : mStaticEntities.entityMap().entrySet()) {
      String id = entry.getKey();
      RemoteEntityInfo source = entry.getValue();

      RemoteEntityInfo origTarget = mDynamicEntities.entityMap().get(id);
      RemoteEntityInfo logOriginal = origTarget;

      if (origTarget == null) {
        log("Adding missing hidden entity:", id);
        origTarget = RemoteEntityInfo.DEFAULT_INSTANCE;
      }

      RemoteEntityInfo.Builder b = source.toBuilder();

      b.port(origTarget.port());
      b.url(origTarget.url());
      RemoteEntityInfo updatedTarget = b.build();
      if (verbose() && !origTarget.equals(updatedTarget)) {
        log("Fixing dynamic entity to agree with static; was:", INDENT, logOriginal, OUTDENT, "now:", INDENT,
            updatedTarget);
      }
      updatedHiddenEntities.put(id, updatedTarget);
    }
    mDynamicEntities.entityMap(updatedHiddenEntities);
  }

  //  /**
  //   * If there are any entries that are missing from the hidden register, add
  //   * them
  //   */
  //  private void fixMissingEntries() {
  //    Map<String, RemoteEntityInfo> modified = hashMap();
  //    modified.putAll(mMutableEntities.entityMap());
  //    for (Map.Entry<String, RemoteEntityInfo> entry : mMutableEntitiesStatic.entityMap().entrySet()) {
  //      if (!mMutableEntities.entityMap().containsKey(entry.getKey()))
  //        modified.put(entry.getKey(), entry.getValue());
  //    }
  //    mMutableEntities.entityMap(modified);
  //  }

  //  /**
  //   * Process all dynamic entities, applying defaults in case any are missing
  //   */
  //  private void updateDynamicEntities() {
  //    Map<String, RemoteEntityInfo> modified = hashMap();
  //    for (Map.Entry<String, RemoteEntityInfo> entry : mMutableEntities.entityMap().entrySet()) {
  //      RemoteEntityInfo filtered = applyDefaults(entry.getKey(), entry.getValue());
  //      modified.put(filtered.id(), filtered.build());
  //    }
  //    mMutableEntities.entityMap(modified);
  //  }

  private boolean mNestedCallFlag;

  //  private RemoteEntityCollection staticEntities() {
  //    dynamicRegister();
  //    return mStaticEntities;
  //  }

  private void flushChanges() {
    RemoteEntityCollection updated;

    updated = dynamicRegister().build();

    if (!updated.equals(mOriginalEntitiesDynamic)) {
      File file = dynamicEntityFile();
      log("...flushing (dynamic) changes to:", file);
      mOriginalEntitiesDynamic = dynamicRegister().build();
      files().writePretty(file, mOriginalEntitiesDynamic);
    }

    updateStaticEntityList();

    updated = staticRegister().build();
    if (!updated.equals(mOriginalEntitiesStatic)) {
      File file = staticEntityFile();
      log("...flushing (static) changes to:", file);
      mOriginalEntitiesStatic = updated;
      files().writePretty(file, updated);
    }
  }

  // Copy all the dynamic entities to the static map, after removing dynamic fields
  private void updateStaticEntityList() {
    Map<String, RemoteEntityInfo> modified;
    modified = hashMap();
    for (RemoteEntityInfo ent : mDynamicEntities.entityMap().values()) {
      modified.put(ent.id(), clearDynamicFields(ent)//
          .port(0) //
          .url(null) //
          .build());
    }
    staticRegister().entityMap(modified);
  }

  private RemoteEntityCollection mOriginalEntitiesDynamic;
  private RemoteEntityCollection mOriginalEntitiesStatic;
  private RemoteEntityCollection.Builder mDynamicEntities;
  private RemoteEntityCollection.Builder mStaticEntities;

}
