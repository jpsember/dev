package dev;

import java.io.File;

/**
 * Interface to a filesystem that acts as an external storage device, e.g. AWS
 * S3
 */
public interface ArchiveDevice {

  boolean fileExists(String name);

  void write(File source, String name);

  void read(String name, File destination);
}
