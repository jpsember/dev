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
import js.base.BaseObject;
import js.base.SystemCall;
import js.file.FileException;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;

public class S3Archive extends BaseObject implements ArchiveDevice {

  public S3Archive(String profileName, String bucketName, String subfolderPath, File projectDirectoryOrNull) {
    checkArgument(RegExp.patternMatchesString("^\\w+(?:\\.\\w+)*(?:\\/\\w+(?:\\.\\w+)*)*$", bucketName),
        "bucket name should be of form xxx.yyy/aaa/bbb.ccc");
    mProfileName = profileName;
    mBareBucket = bucketName;

    if (!nullOrEmpty(subfolderPath)) {
      checkArgument(!subfolderPath.endsWith("/"), "unexpected trailing / in subfolderPath");
      mSubfolderPrefix = subfolderPath + "/";
    } else
      mSubfolderPrefix = "";
    if (Files.nonEmpty(projectDirectoryOrNull))
      mRootDirectory = Files.assertDirectoryExists(projectDirectoryOrNull, "root directory");
    else
      mRootDirectory = null;
  }

  @Override
  public void setDryRun(boolean dryRun) {
    checkState(mDryRun == null || mDryRun == dryRun, "dry run already initialized");
    mDryRun = dryRun;
  }

  @Override
  public boolean fileExists(String name) {
    SystemCall sc = s3APICall();
    sc.arg("get-object-acl");
    sc.arg("--bucket", mBareBucket);
    sc.arg("--key", mSubfolderPrefix + name);
    return checkSuccess(sc, "(NoSuchKey)");
  }

  private boolean checkSuccess(SystemCall sc, String optionalErrorSubstring) {
    if (sc.exitCode() == 0)
      return true;
    if (!nullOrEmpty(optionalErrorSubstring)) {
      String sysErr = sc.systemErr();
      if (sysErr.contains(optionalErrorSubstring))
        return false;
    }
    pr("*** Error:", sc.systemErr());
    pr("***");
    pr("*** Do you not have access to the S3 account?");
    pr("***");
    throw FileException.withMessage(sc.systemErr());
  }

  @Override
  public void push(File source, String name) {
    if (isDryRun())
      return;
    SystemCall sc = s3APICall();
    sc.arg("put-object");
    sc.arg("--bucket", mBareBucket);
    sc.arg("--key", mSubfolderPrefix + name);
    sc.arg("--body", source.toString());
    checkSuccess(sc, null);
  }

  @Override
  public void pull(String name, File destination) {
    if (isDryRun())
      return;

    SystemCall sc = s3APICall();
    sc.arg("get-object");
    sc.arg("--bucket", mBareBucket);
    sc.arg("--key", mSubfolderPrefix + name);
    sc.arg(destination);

    checkSuccess(sc, null);
  }

  @Override
  public List<CloudFileEntry> listFiles(String prefix) {
    if (isDryRun())
      throw notSupported("not supported in dryrun");

    prefix = mSubfolderPrefix + nullToEmpty(prefix);
    SystemCall sc = s3APICall();
    sc.arg("list-objects-v2");
    sc.arg("--bucket", mBareBucket);
    if (!nullOrEmpty(prefix))
      sc.arg("--prefix", prefix);
    checkSuccess(sc, null);

    JSMap result = new JSMap(sc.systemOut());

    List<CloudFileEntry> fileEntryList = arrayList();
    JSList items = result.getList("Contents");

    for (JSMap m : items.asMaps()) {
      String key = m.get("Key");
      key = chompPrefix(key, mSubfolderPrefix);

      if (key.isEmpty() || key.endsWith("/"))
        continue;

      fileEntryList.add(CloudFileEntry.newBuilder() //
          .name(key) //
          .size(m.getLong("Size"))//
          .build());
    }
    return fileEntryList;
  }

  private SystemCall s3APICall() {
    SystemCall sc = new SystemCall();
    sc.setVerbose(verbose());
    if (mRootDirectory != null)
      sc.directory(mRootDirectory);
    sc.arg("aws", "s3api", "--profile", mProfileName);
    return sc;
  }

  private boolean isDryRun() {
    if (mDryRun == null)
      setDryRun(false);
    return mDryRun;
  }

  private final String mProfileName;
  private final File mRootDirectory;
  private final String mSubfolderPrefix;
  private final String mBareBucket;
  private Boolean mDryRun;
}
