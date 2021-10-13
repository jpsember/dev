package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.Map;

import dev.gen.RemoteEntityInfo;
import dev.gen.OsType;
import dev.gen.RemoteEntityCollection;
import js.base.BaseObject;
import js.file.Files;

/**
 * <pre>
 * 
 * Files of note
 * 
 * Static registry
 * =================
 * This is a RemoteEntityCollection that represents the known remote entities,
 * but without the fields that are likely to change frequently, specifically the
 * ngrok port and urls, and the 'active' entity id. It is to be tracked by git.
 * 
 * Dynamic registry
 * =================
 * This is a RemoteEntityCollection that mirrors the static registry, except
 * that it also contains the ngrok port and url for the entities (which may be
 * out of date), as well as the 'active' entity id. It is not to be tracked by
 * git. The EntityManager frequently will update this registry, to make sure it
 * mirrors the static registry (subject to the noted exceptions).
 * 
 * </pre>
 */
public class EntityManager extends BaseObject {

  public static EntityManager sharedInstance() {
    if (sSharedInstance == null)
      sSharedInstance = new EntityManager();
    return sSharedInstance;
  }

  private static EntityManager sSharedInstance;

  //------------------------------------------------------------------
  // Supplying Files object, to support dryrun operation
  // ------------------------------------------------------------------

  /**
   * To support dryrun operation, injectable Files object (initially Files.S)
   */
  public EntityManager withFiles(Files files) {
    mFiles = files;
    return this;
  }

  /**
   * Get an immutable copy of the current (dynamic) entities
   */
  public RemoteEntityCollection currentEntities() {
    return dynamicRegistry().build();
  }

  public RemoteEntityInfo optionalActiveEntity() {
    return entryFor(dynamicRegistry().activeEntity());
  }

  /**
   * Get RemoteEntryInfo for a particular key. Return default entity if key is
   * null or empty
   */
  public RemoteEntityInfo entryFor(String key) {
    RemoteEntityInfo ent = optionalEntryFor(key);
    if (!nullOrEmpty(key) && ent == null)
      throw badState("No entity found for key:", key);
    return ent;
  }

  public RemoteEntityInfo optionalEntryFor(String key) {
    RemoteEntityInfo ent = null;
    if (!nullOrEmpty(key))
      ent = dynamicRegistry().entityMap().get(key);
    return ent;
  }

  public RemoteEntityInfo activeEntity() {
    RemoteEntityInfo ent = optionalActiveEntity();
    if (ent == RemoteEntityInfo.DEFAULT_INSTANCE)
      throw badState("No active remote entity");
    return ent;
  }

  public void setActive(String tag) {
    if (!dynamicRegistry().entityMap().containsKey(tag))
      throw badArg("entity not found:", tag);
    dynamicRegistry().activeEntity(tag);
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
    RemoteEntityInfo template = dynamicRegistry().entityMap().get(entity.id());
    boolean added = (template == null);
    if (added) {
      template = staticRegistry().entityTemplate().toBuilder().id(entity.id());
    }

    // Apply defaults from this template
    //
    entity = applyDefaults(entity.id(), entity, template).build();

    // If hidden entity already exists and is not changed, we don't need to continue
    //
    RemoteEntityInfo prevOrNull = dynamicRegistry().entityMap().get(entity.id());
    if (entity.equals(prevOrNull)) {
      log("...no changes");
    } else {
      log("...storing", added ? "new" : "modified", "entity:", INDENT, entity);
      dynamicRegistry().entityMap().put(entity.id(), entity);
      // Store appropriate version within static register
      RemoteEntityInfo staticVersion = clearDynamicFields(entity).build();
      log("...storing static version:", INDENT, staticVersion);
      staticRegistry().entityMap().put(entity.id(), staticVersion);
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
    builder.id(id);
    if (builder.osType() == OsType.UNKNOWN)
      builder.osType(template.osType());
    if (builder.user().isEmpty())
      builder.user(template.user());
    if (Files.empty(builder.projectDir()))
      builder.projectDir(template.projectDir());
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

  private RemoteEntityCollection.Builder staticRegistry() {
    dynamicRegistry();
    return mStaticRegistry;
  }

  private RemoteEntityCollection.Builder dynamicRegistry() {
    if (mDynamicRegistry == null) {
      checkState(!mNestedCallFlag);
      mNestedCallFlag = true;

      // We must read both registers before attempting any edits, so that the
      // instance fields are initialized
      //
      readStaticRegister();
      readDynamicRegister();

      fixStaticRegistry();
      fixDynamicEntries();

      // Flush any changes immediately, in case client isn't going to make any changes
      flushChanges();
      mNestedCallFlag = false;
    }
    return mDynamicRegistry;
  }

  private void readDynamicRegister() {
    mOriginalDynamicRegistry = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        dynamicEntityFile());
    mDynamicRegistry = mOriginalDynamicRegistry.toBuilder();
  }

  private void readStaticRegister() {
    mOriginalStaticRegistry = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
        staticEntityFile());
    mStaticRegistry = mOriginalStaticRegistry.toBuilder();
  }

  /**
   * Process the static entries, applying any fixes; e.g. mismatched id, missing
   * default values
   */
  private void fixStaticRegistry() {
    Map<String, RemoteEntityInfo> updatedMap = hashMap();
    for (Map.Entry<String, RemoteEntityInfo> entry : staticRegistry().entityMap().entrySet()) {
      String id = entry.getKey();
      RemoteEntityInfo original = entry.getValue();
      RemoteEntityInfo fixed = applyDefaults(id, original, mStaticRegistry.entityTemplate()).build();
      if (verbose() && !original.equals(fixed))
        log("Fixed entity:", id, INDENT, fixed);
      updatedMap.put(id, fixed);
    }
    staticRegistry().entityMap(updatedMap);
  }

  /**
   * Update all dynamic entries to agree with their static counterparts, adding
   * any that are missing
   */
  private void fixDynamicEntries() {
    Map<String, RemoteEntityInfo> updatedMap = hashMap();

    for (Map.Entry<String, RemoteEntityInfo> entry : mStaticRegistry.entityMap().entrySet()) {
      String id = entry.getKey();
      RemoteEntityInfo source = entry.getValue();

      RemoteEntityInfo origTarget = dynamicRegistry().entityMap().get(id);
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
      updatedMap.put(id, updatedTarget);
    }
    dynamicRegistry().entityMap(updatedMap);

    String activeId = dynamicRegistry().activeEntity();
    if (!nullOrEmpty(activeId)) {
      if (optionalEntryFor(activeId) == null) {
        log("Active entity doesn't exist, clearing:", activeId);
        dynamicRegistry().activeEntity(null);
      }
    }
  }

  private void flushChanges() {
    RemoteEntityCollection updated;

    updated = dynamicRegistry().build();

    if (!updated.equals(mOriginalDynamicRegistry)) {
      File file = dynamicEntityFile();
      log("...flushing (dynamic) changes to:", file);
      mOriginalDynamicRegistry = dynamicRegistry().build();
      files().writePretty(file, mOriginalDynamicRegistry);
    }

    updateStaticEntityList();

    updated = staticRegistry().build();
    if (!updated.equals(mOriginalStaticRegistry)) {
      File file = staticEntityFile();
      log("...flushing (static) changes to:", file);
      mOriginalStaticRegistry = updated;
      files().writePretty(file, updated);
    }
  }

  // Copy all the dynamic entities to the static map, after removing dynamic fields
  private void updateStaticEntityList() {
    Map<String, RemoteEntityInfo> modified;
    modified = hashMap();
    for (RemoteEntityInfo ent : dynamicRegistry().entityMap().values()) {
      modified.put(ent.id(), clearDynamicFields(ent)//
          .port(0) //
          .url(null) //
          .build());
    }
    staticRegistry().entityMap(modified);
  }

  private Files files() {
    return mFiles;
  }

  private RemoteEntityCollection mOriginalDynamicRegistry;
  private RemoteEntityCollection mOriginalStaticRegistry;
  private RemoteEntityCollection.Builder mDynamicRegistry;
  private RemoteEntityCollection.Builder mStaticRegistry;

  // For ensuring we aren't making nested calls to the method that loads and 
  // 'fixes up' the registers
  //
  private boolean mNestedCallFlag;

  private Files mFiles = Files.S;

}
