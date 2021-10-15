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

import js.base.SystemCall;
import js.file.Files;
import js.parsing.RegExp;

public class S3Archive implements ArchiveDevice {

  public S3Archive(String profileName, String bucketName, File projectDirectory) {
    checkArgument(RegExp.patternMatchesString("^\\w+(?:\\.\\w+)*(?:\\/\\w+(?:\\.\\w+)*)*$", bucketName),
        "bucket name should be of form xxx.yyy/aaa/bbb.ccc");
    mProfileName = profileName;
    mBucketPath = "s3://" + bucketName + "/";
    mRootDirectory = Files.assertDirectoryExists(projectDirectory, "root directory");
  }

  @Override
  public void setDryRun(boolean dryRun) {
    checkState(mDryRun == null || mDryRun == dryRun, "dry run already initialized");
    mDryRun = dryRun;
  }

  @Override
  public boolean fileExists(String name) {
    SystemCall sc = s3Call();
    sc.arg("ls", mBucketPath + name);
    return sc.systemOut().contains(name);
  }

  @Override
  public void push(File source, String name) {
    if (isDryRun())
      return;
    SystemCall sc = s3Call();
    sc.arg("cp", source.toString(), mBucketPath + name);
    sc.assertSuccess();
  }

  @Override
  public void pull(String name, File destination) {
    if (isDryRun())
      return;
    SystemCall sc = s3Call();
    sc.arg("cp", mBucketPath + name, destination);
    if (sc.exitCode() != 0) {
      if (sc.systemErr().contains("Forbidden")) {
        pr("***");
        pr("*** Do you not have access to the S3 account?");
        pr("***");
      }
    }
    sc.assertSuccess();
  }

  private SystemCall s3Call() {
    SystemCall sc = new SystemCall();
    sc.directory(mRootDirectory);
    sc.arg("aws", "s3", "--profile", mProfileName);
    return sc;
  }

  private boolean isDryRun() {
    if (mDryRun == null)
      setDryRun(false);
    return mDryRun;
  }

  private final String mProfileName;
  private final String mBucketPath;
  private final File mRootDirectory;
  private Boolean mDryRun;
}
