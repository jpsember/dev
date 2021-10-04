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
import java.util.Arrays;
import java.util.List;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;

public class SetupMachineOper extends AppOper {

  private static final boolean LOCAL_TEST = true
      && alert("allowing running from local machine for test purposes");

  @Override
  public String userCommand() {
    return "setup";
  }

  @Override
  public String getHelpDescription() {
    return "setup system";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return null;
    //return arrayList("[<xxx>]", "[<yyy>]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (false) {
      if (args.hasNextArg()) {
      }
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    validateOS();
    prepareSSH();
    prepareVI();
  }

  private void validateOS() {
    String osName = System.getProperty("os.name", "<none>");
    if (osName.equals("<none>"))
      throw badState("Unknown os.name:", osName);
    if (osName.contains("OS X")) {
      if (!LOCAL_TEST)
        throw badState("Unexpected os.name:", osName, "(are you running this on your Mac by mistake?");
      mLocalTest = true;
    }
  }

  private void prepareSSH() {
    log("...prepareSSH");
    File sshDir = new File(Files.homeDirectory(), ".ssh");
    if (mLocalTest)
      sshDir = new File(Files.homeDirectory(), "_temp_ssh");
    files().mkdirs(sshDir);

    byte[] content = Files.toByteArray(fileWithinSecrets("authorized_keys.txt"));
    File authorizedKeys = new File(sshDir, "authorized_keys");

    writeWithBackup(authorizedKeys, content);
  }

  private void prepareVI() {
    log("...prepareVI");
    File homeDir = Files.homeDirectory();
    if (mLocalTest) {
      homeDir = new File(Files.homeDirectory(), "_temp_home");
      files().mkdirs(homeDir);
    }

    byte[] content = Files.toByteArray(fileWithinSecrets("vimrc.txt"));
    File targetFile = new File(homeDir, ".vimrc");
    writeWithBackup(targetFile, content);
  }

  /**
   * If a file exists, create backup copy. Use an incrementing filename to avoid
   * overwriting a previous backup. Doesn't create an additional backup if file
   * hasn't changed since the most recent backup
   * 
   * TODO: doesn't attempt to preserve original file's permissions
   * 
   * @return true if file
   */
  private void writeWithBackup(File targetFile, byte[] newContents) {

    String backupsPrefix;
    {
      String file = targetFile.getName();
      if (file.startsWith("."))
        backupsPrefix = file;
      else {
        String basename = Files.basename(file);
        String ext = Files.getExtension(targetFile);
        if (!ext.isEmpty())
          backupsPrefix = basename + "_" + ext;
        else
          backupsPrefix = basename;
      }
    }

    File backupFile = null;
    for (int candidateIndex = 0;; candidateIndex++) {
      File file = new File(Files.parent(targetFile),
          String.format("%s_%02d.bak", backupsPrefix, candidateIndex));
      if (!file.exists()) {
        backupFile = file;
        break;
      }
    }

    if (targetFile.exists()) {
      checkArgument(targetFile.isFile(), "not a file:", targetFile);
      byte[] originalContents = Files.toByteArray(targetFile);
      if (Arrays.equals(originalContents, newContents))
        return;
      log("...backing up to:", backupFile);
      files().moveFile(targetFile, backupFile);
    }
    log("...writing new contents to:", backupFile);
    files().write(newContents, targetFile);
  }

  private File fileWithinSecrets(String relativePath) {
    checkArgument(!relativePath.startsWith("/"), "expected relative path:", relativePath);
    File file = new File(files().projectSecretsDirectory(), relativePath);
    if (!file.exists())
      throw badState("File", relativePath, "not found in secrets directory:", INDENT,
          files().projectSecretsDirectory());
    return file;
  }

  private boolean mLocalTest;
}
