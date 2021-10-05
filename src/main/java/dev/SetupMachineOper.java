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
import js.base.SystemCall;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;

public class SetupMachineOper extends AppOper {

  @Override
  public String userCommand() {
    return "setup";
  }

  @Override
  public String getHelpDescription() {
    return "setup system";
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();

    if (args.hasNextArg()) {
      String arg = args.nextArg();
      switch (arg) {
      case "eclipse":
        mEclipseMode = true;
        break;
      default:
        setError("Unsupported argument:", arg);
        break;
      }
    }

    args.assertArgsDone();
  }

  @Override
  public void perform() {
    validateOS();
    prepareSSH();
    prepareBash();
    prepareVI();
    prepareGit();
    prepareGitHub();
    prepareAWS();
    runSetupScript();
  }

  private void validateOS() {
    mEntityName = Files.readString(fileWithinSecrets("entity_name.txt")).trim();
    mLocalTest = mEntityName.equals("osx");
  }

  private void prepareSSH() {
    log("...prepareSSH");
    File sshDir = fileWithinHome(".ssh");
    files().mkdirs(sshDir);
    writeWithBackup(new File(sshDir, "authorized_keys"), fileWithinSecrets("authorized_keys.txt"));

    // Install public/private key pair for accessing GitHub
    //
    writeWithBackup(new File(sshDir, "id_rsa.pub"), fileWithinSecrets("id_rsa.pub.txt"));
    writeWithBackup(new File(sshDir, "id_rsa"), fileWithinSecrets("id_rsa.txt"));
  }

  private void prepareVI() {
    log("...prepareVI");
    writeWithBackup(fileWithinHome(".vimrc"), resourceString("vimrc.txt"));
  }

  private void prepareGit() {
    log("...prepareGit");
    writeWithBackup(fileWithinHome(".gitconfig"), resourceString("git_config.txt"));
  }

  private void prepareGitHub() {
    log("...prepareGitHub");
    todo("prepareGitHub");
  }

  private void prepareAWS() {
    log("...prepareAWS");
    File awsDir = fileWithinHome(".aws");
    files().mkdirs(awsDir);

    writeWithBackup(new File(awsDir, "config"), fileWithinSecrets("aws_config.txt"));
    writeWithBackup(new File(awsDir, "credentials"), fileWithinSecrets("aws_credentials.txt"));
  }

  private String assertRelative(String path) {
    if (nullOrEmpty(path) || path.startsWith("/"))
      throw badArg("Not a relative path:", path);
    return path;
  }

  private void prepareBash() {
    log("...prepareBash");
    writeWithBackup(fileWithinHome(".inputrc"), fileWithinSecrets("inputrc.txt"));
    writeWithBackup(fileWithinHome(".entity_name.txt"), mEntityName);

    File profileFile = assertExists(fileWithinHome(".profile"));

    final String customFilename = ".profile_custom";
    String newContent;
    {
      String text = Files.readString(profileFile);
      String delimText = "# <<< generated code; do not edit\n";

      List<Integer> delims = arrayList();
      int cursor = 0;
      while (cursor < text.length()) {
        int loc = text.indexOf(delimText, cursor);
        if (loc < 0)
          break;
        delims.add(loc);
        cursor = loc + delimText.length();
      }
      if (delims.size() != 0 && delims.size() != 2)
        throw badState("unexpected occurrences of delimeter text in", profileFile);

      {
        String insertContent = "source ~/" + customFilename + "\n";
        if (delims.size() == 0) {
          newContent = text + "\n" + delimText + insertContent + delimText;
        } else {
          newContent = text.substring(0, delims.get(0)) + delimText + insertContent + delimText
              + text.substring(delims.get(1) + delimText.length());
        }
      }
    }
    writeWithBackup(profileFile, applyMacroParser(newContent));
    writeWithBackup(fileWithinHome(customFilename), applyMacroParser(resourceString("profile_custom.txt")));
  }

  private String resourceString(String filename) {
    return Files.readString(getClass(), filename);
  }

  private String applyMacroParser(String sourceText) {
    if (mMacroMap == null) {
      mMacroMap = map()//
          .put("entity_id", mEntityName);
    }
    return MacroParser.parse(sourceText, mMacroMap);
  }

  private JSMap mMacroMap;

  private void runSetupScript() {
    String scriptFilename = ".setup_script.sh";
    File targetFile = fileWithinHome(scriptFilename);
    writeWithBackup(targetFile, applyMacroParser(resourceString("setup_script.txt")));

    pr("Running script that requires sudo access... type password if necessary...");
    SystemCall sc = new SystemCall();
    sc.arg("bash", targetFile);
    sc.setVerbose();
    if (mEclipseMode) {
      pr("...not running script, since eclipse mode is active");
    } else
      sc.assertSuccess();
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
      log("...backing up old version");
      files().moveFile(targetFile, backupFile);
    }
    files().write(newContents, targetFile);
  }

  private void writeWithBackup(File targetFile, String newContent) {
    writeWithBackup(targetFile, newContent.getBytes());
  }

  private void writeWithBackup(File targetFile, File sourceFile) {
    writeWithBackup(targetFile, Files.toByteArray(sourceFile));
  }

  /**
   * Make sure file exists
   */
  private File assertExists(File file) {
    if (!file.exists())
      throw badState("File", file, "doesn't exist");
    return file;
  }

  /**
   * Get a file within the secrets directory; make sure it exists
   */
  private File fileWithinSecrets(String relativePath) {
    File file = new File(files().projectSecretsDirectory(), assertRelative(relativePath));
    return assertExists(file);
  }

  /**
   * Get a file within the (effective) home directory
   */
  private File fileWithinHome(String relativePath) {
    return new File(effectiveHomeDir(), assertRelative(relativePath));
  }

  /**
   * Get the 'effective' home directory. This is Files.homeDirectory() unless
   * we're running in mLocalTest mode
   */
  private File effectiveHomeDir() {
    if (mEffectiveHomeDir == null) {
      File homeDir = Files.homeDirectory();
      if (mLocalTest) {
        homeDir = new File(Files.homeDirectory(), "_temp_home");
        files().mkdirs(homeDir);
      }
      mEffectiveHomeDir = homeDir;
    }
    return mEffectiveHomeDir;
  }

  private String mEntityName;
  private File mEffectiveHomeDir;
  private boolean mEclipseMode;

  // Have this start of null, to ensure we initialize it before attempting to use it
  private Boolean mLocalTest;
}
