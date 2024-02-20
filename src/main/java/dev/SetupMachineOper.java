/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
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
import js.webtools.gen.RemoteEntityInfo;

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
      case "new":
        mNewFlag = true;
        break;
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
    if (mNewFlag) {
      performNew();
      return;
    }

    validateOS();
    prepareSSH();
    prepareBash();
    prepareVI();
    prepareScreen();
    prepareGit();
    prepareAWS();
    if (false)
      verifyPython();
    verifyJava();
    runSetupScript();
  }

  private void validateOS() {
  }

  private void prepareSSH() {
    todo("see latest error related to git pull on remote machine");
    log("...prepareSSH");
    File sshDir = fileWithinHome(".ssh");
    files().mkdirs(sshDir);
    writeWithBackup(new File(sshDir, "authorized_keys"), files().fileWithinSecrets("authorized_keys.txt"));

    // Install public/private key pairs
    //
    String[] keyNames = { "id_rsa", "cowmarker" };
    for (String keyName : keyNames) {
      writeWithBackup(new File(sshDir, keyName + ".pub"), files().fileWithinSecrets(keyName + ".pub.txt"));
      File targetFile = new File(sshDir, keyName);
      writeWithBackup(targetFile, files().fileWithinSecrets(keyName + ".txt"));
      // The permissions for the private key must be restricted or the ssh program complains
      //
      files().chmod(targetFile, 600);
    }
  }

  private void prepareVI() {
    log("...prepareVI");
    writeWithBackup(fileWithinHome(".vimrc"), resourceString("vimrc.txt"));
  }

  private void prepareScreen() {
    log("...prepareScreen");
    writeWithBackup(fileWithinHome(".screenrc"), resourceString("screenrc.txt"));
  }

  private void prepareGit() {
    log("...prepareGit");
    writeWithBackup(fileWithinHome(".gitconfig"), applyMacroParser(resourceString("git_config.txt")));
  }

  private void prepareAWS() {
    log("...prepareAWS");
    File awsDir = fileWithinHome(".aws");
    files().mkdirs(awsDir);

    writeWithBackup(new File(awsDir, "config"), files().fileWithinSecrets("aws_config.txt"));
    writeWithBackup(new File(awsDir, "credentials"), files().fileWithinSecrets("aws_credentials.txt"));
  }

  private void verifyPython() {
    log("...verifying Python version");
    SystemCall sc = new SystemCall().withVerbose(verbose()).arg("python3", "--version");
    try {
      sc.assertSuccess();
    } catch (RuntimeException e) {
      pr(e);
      setError("Python 3 verification failed");
    }
    if (!sc.systemOut().contains("Python 3.7"))
      pr("*** Unexpected python version:", INDENT, sc.systemOut());
  }

  private void verifyJava() {
    log("...verifying Java version");
    String version = System.getProperty("java.version");
    if (!version.startsWith("1.8."))
      pr("*** Unexpected Java version:", version);
  }

  private String assertRelative(String path) {
    if (nullOrEmpty(path) || path.startsWith("/"))
      throw badArg("Not a relative path:", path);
    return path;
  }

  private void prepareBash() {
    log("...prepareBash");

    // Create ~/bin directory 
    //
    files().mkdirs(fileWithinHome("bin"));

    writeWithBackup(fileWithinHome(".inputrc"), files().fileWithinSecrets("inputrc.txt"));

    // There are two bash configuration files we are interested in:
    //
    // .profile          : a standard file read by bash when shell scripts are run
    // .profile_custom   : an additional configuration file that we will create, and will be included by .profile
    //
    final String customFilename = ".profile_custom";

    File bashProfileMain = assertExists(fileWithinHome(".profile"));
    File bashProfileAux = fileWithinHome(customFilename);

    // Append (or modify existing) text to include the auxilliary profile from the main one
    //
    String newContent;
    {
      String text = Files.readString(bashProfileMain);
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
        throw badState("unexpected occurrences of delimeter text in", bashProfileMain);

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
    writeWithBackup(bashProfileMain, applyMacroParser(newContent));
    writeWithBackup(bashProfileAux, applyMacroParser(resourceString("profile_custom.txt")));
  }

  private String resourceString(String filename) {
    return Files.readString(getClass(), filename);
  }

  private String applyMacroParser(String sourceText) {
    if (mMacroMap == null) {
      RemoteEntityInfo entityInfo = Utils.ourEntityInfo();
      mMacroMap = map()//
          .put("entity_id", entityInfo.label()) //
          .put("project_dir", entityInfo.projectDir().toString()) //
      ;
    }
    return MacroParser.parse(sourceText, mMacroMap);
  }

  private JSMap mMacroMap;

  private void runSetupScript() {
    File targetFile = fileWithinHome(".setup_script.sh");
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

  private File determineBackupFile(File targetFile, List<File> existingBackups) {
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

    File parentFile = Files.parent(targetFile);
    if (existingBackups == null)
      existingBackups = arrayList();

    int highestBackupIndex = -1;
    String seekPrefix = backupsPrefix + "_";
    for (File existingBackupFile : Files.filesWithExtension(parentFile, "bak")) {
      String name = Files.basename(existingBackupFile);
      if (name.startsWith(seekPrefix)) {
        existingBackups.add(existingBackupFile);
        int indexPosition = 1 + name.lastIndexOf('_');
        String substr = name.substring(indexPosition);
        int backupIndex = Integer.parseInt(substr);
        highestBackupIndex = Math.max(highestBackupIndex, backupIndex);
      }
    }

    String backupName = String.format("%s%02d.bak", seekPrefix, highestBackupIndex + 1);
    File backupFile = new File(parentFile, backupName);
    return backupFile;
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
    List<File> existingBackups = arrayList();
    File backupFile = determineBackupFile(targetFile, existingBackups);

    if (targetFile.exists()) {
      checkArgument(targetFile.isFile(), "not a file:", targetFile);
      byte[] originalContents = Files.toByteArray(targetFile, "SetupMachineOper.writeWithBackup");
      if (Arrays.equals(originalContents, newContents))
        return;
      log("...backing up old version");
      files().moveFile(targetFile, backupFile);
      existingBackups.add(backupFile);
    }

    files().write(newContents, targetFile);

    // Trim existing backups to reasonable size
    final int MAX_BACKUPS = 3;

    for (int i = 0; i < existingBackups.size() - MAX_BACKUPS; i++)
      files().deleteFile(existingBackups.get(i));
  }

  private void writeWithBackup(File targetFile, String newContent) {
    writeWithBackup(targetFile, newContent.getBytes());
  }

  private void writeWithBackup(File targetFile, File sourceFile) {
    writeWithBackup(targetFile, Files.toByteArray(sourceFile, "SetupMachineOper.writeWithBackup"));
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
   * Get a file within the (effective) home directory
   */
  private File fileWithinHome(String relativePath) {
    return new File(Files.homeDirectory(), assertRelative(relativePath));
  }

  private void performNew() {
    // Create ~/bin directory 
    //
    var binDir = files().mkdirs(fileWithinHome("bin"));

    // Create ~/bin/mk script
    //
    var targetFile = new File(binDir, "mk");
    files().writeString(targetFile, resourceString("bin_mk_template.txt"));
    files().chmod(targetFile, 744);
  }

  private boolean mEclipseMode;
  private boolean mNewFlag;
}
