package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import js.base.BaseObject;
import js.file.Files;

public final class Zipper extends BaseObject {

  public Zipper(Files f) {
    updateVerbose();
    if (f == null)
      f = Files.S;
    mFiles = f;
  }

  public void open(File zipFile) {
    log("openForWriting:", zipFile);
    checkState(mZipFile == null);
    checkArgument(Files.getExtension(Files.assertNonEmpty(zipFile, "zipFile arg")).equals(Files.EXT_ZIP),
        zipFile, "not a zip file");
    mZipFile = zipFile;
    mFiles.deletePeacefully(zipFile);
    mOutputStream = new ZipOutputStream(mFiles.outputStream(zipFile));
  }

  @Deprecated
  public void addEntry(File file, String name) {
    checkState(mOutputStream != null);
    byte[] bytes = Files.toByteArray(file, "zipping");
    if (nullOrEmpty(name))
      name = file.toString();

    ZipEntry zipEntry = new ZipEntry(name);
    try {
      mOutputStream.putNextEntry(zipEntry);
      mOutputStream.write(bytes);
      mOutputStream.closeEntry();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  public void addEntry(String name, byte[] bytes) {
    checkState(mOutputStream != null);
    ZipEntry zipEntry = new ZipEntry(name);
    try {
      mOutputStream.putNextEntry(zipEntry);
      mOutputStream.write(bytes);
      mOutputStream.closeEntry();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  public void close() {
    checkState(mOutputStream != null);
    try {
      mOutputStream.close();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
    mOutputStream = null;
  }

  private final Files mFiles;
  private File mZipFile;
  private ZipOutputStream mOutputStream;
}
