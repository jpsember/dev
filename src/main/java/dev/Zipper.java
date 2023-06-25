package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import js.base.BaseObject;
import js.data.AbstractData;
import js.data.DataUtil;
import js.file.Files;

public final class Zipper extends BaseObject {

  public Zipper(Files f) {
    updateVerbose();
    if (f == null)
      f = Files.S;
    mFiles = f;
  }

  public void open(File zipFile) {
    log("open:", zipFile);
    checkState(mTempZipFile == null);
    checkArgument(Files.getExtension(Files.assertNonEmpty(zipFile, "zipFile arg")).equals(Files.EXT_ZIP),
        zipFile, "not a zip file");
    mFinalZipFile = zipFile;
    mTempZipFile = Files.createTempFile("zipper_", ".zip");
    mFiles.deletePeacefully(mFinalZipFile);
    mOutputStream = new ZipOutputStream(mFiles.outputStream(mTempZipFile));
  }

  public void addEntry(String name, String string) {
    addEntry(name, string.getBytes());
  }

  public void addEntry(String name, File file) {
    addEntry(name, Files.toByteArray(file, "addEntry"));
  }

  public void addEntry(String name, AbstractData data) {
    addEntry(name, DataUtil.toByteArray(data));
  }

  public void addEntry(String name, byte[] bytes) {
    log("addEntry:", name);
    checkState(!mEntries.contains(name));
    checkState(mOutputStream != null);
    ZipEntry zipEntry = new ZipEntry(name);
    try {
      mOutputStream.putNextEntry(zipEntry);
      mOutputStream.write(bytes);
      mOutputStream.closeEntry();
      mEntries.add(name);
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  public void close() {
    checkState(mOutputStream != null);
    try {
      mOutputStream.close();
      mFiles.moveFile(mTempZipFile, mFinalZipFile);
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
    mOutputStream = null;
  }

  public boolean contains(String name) {
    return mEntries.contains(name);
  }

  private final Files mFiles;
  private File mTempZipFile, mFinalZipFile;
  private ZipOutputStream mOutputStream;
  private Set<String> mEntries = hashSet();
}
