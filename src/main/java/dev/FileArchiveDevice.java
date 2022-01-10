/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package dev;

import static js.base.Tools.*;

import java.io.File;
import java.util.List;

import dev.gen.CloudFileEntry;
import js.file.Files;

public class FileArchiveDevice implements ArchiveDevice {

  public FileArchiveDevice(File rootDirectory) {
    loadTools();
    mRootDir = rootDirectory;
  }

  @Override
  public void setDryRun(boolean dryRun) {
    checkState(mFiles == null, "files already constructed");
    mDryRun = dryRun;
  }

  @Override
  public boolean fileExists(String name) {
    return fileWithinArchive(name).exists();
  }

  private File fileWithinArchive(String name) {
    return new File(mRootDir, name);
  }

  @Override
  public void push(File source, String name) {
    File target = fileWithinArchive(name);
    files().mkdirs(Files.parent(target));
    files().copyFile(source, target);
  }

  @Override
  public void pull(String name, File destination) {
    File source = fileWithinArchive(name);
    files().copyFile(source, destination);
  }

  @Override
  public List<CloudFileEntry> listFiles(String prefix) {
    throw notFinished();
  }

  private Files files() {
    if (mFiles == null) {
      Files mf = new Files();
      mf.withDryRun(mDryRun);
      mf.mkdirs(mRootDir);
      mFiles = mf;
    }
    return mFiles;
  }

  private final File mRootDir;
  private boolean mDryRun;
  private Files mFiles;
}
