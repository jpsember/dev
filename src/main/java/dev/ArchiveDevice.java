package dev;

import java.io.File;

/**
 * Interface to a filesystem that acts as an external storage device, e.g. AWS
 * S3
 */
public interface ArchiveDevice {

  /**
   * Determine if an object exists in the archive
   */
  boolean fileExists(String name);

  /**
   * Push a local object to the archive
   */
  void push(File source, String name);

  /**
   * Pull an object from the archive to the local machine
   */
  void pull(String name, File destination);
}