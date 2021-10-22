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

import dev.gen.RemoteEntityInfo;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.Files;

public class SecretsOper extends AppOper {

  @Override
  public String userCommand() {
    return "secrets";
  }

  @Override
  public String getHelpDescription() {
    return "send secrets directory to remote machine";
  }

  private static final String SECRETS_DIR_NAME = "_secrets_temp_";

  @Override
  public void perform() {

    RemoteEntityInfo ent = EntityManager.sharedInstance().activeEntity();

    {
      // Construct a modified version of the secrets directory, one with a customized entity name
      mSecretsWorkDir = new File(Files.getDesktopDirectory(), SECRETS_DIR_NAME);
      discardSecretsWorkDir();
      files().mkdirs(mSecretsWorkDir);

      File sourceDir = files().projectSecretsDirectory();

      files().copyDirectory(sourceDir, mSecretsWorkDir);

      File entityInfoFile = new File(mSecretsWorkDir, Files.SECRETS_FILE_ENTITY_INFO);
      files().writePretty(entityInfoFile, ent);

      //      if (false) {
      //        File entityNameFile = new File(mSecretsWorkDir, Files.SECRETS_FILE_ENTITY_NAME);
      //        checkState(entityNameFile.exists(), "did not find:", entityNameFile);
      //        files().writeString(entityNameFile, ent.id());
      //      }
    }

    SystemCall s = new SystemCall();
    boolean verbosity = verbose();
    s.withVerbose(verbosity);
    s.arg("rsync");
    s.arg("--archive");

    if (verbosity)
      s.arg("--verbose");
    if (dryRun())
      s.arg("--dry-run");

    String sourceDirString = mSecretsWorkDir.toString() + "/"; // Copy the contents of the directory, since target already exists
    s.arg(sourceDirString);
    checkArgument(ent.port() > 0, "bad port:", INDENT, ent);
    s.arg("-e", "ssh -p" + ent.port());

    // Determine the target directory

    String targetDirString;
    targetDirString = new File(ent.projectDir(), "secrets").toString();

    {
      StringBuilder sb = new StringBuilder();

      checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
      checkArgument(nonEmpty(ent.url()), "no url:", INDENT, ent);
      sb.append(ent.user());
      sb.append('@');
      sb.append(ent.url());
      sb.append(':');

      sb.append(targetDirString);
      s.arg(sb);
    }

    s.call();
    s.assertSuccess();
    if (!verbose())
      discardSecretsWorkDir();
  }

  private void discardSecretsWorkDir() {
    String name = mSecretsWorkDir.getName();
    checkArgument(name.equals(SECRETS_DIR_NAME));
    files().deleteDirectory(mSecretsWorkDir);
  }

  private File mSecretsWorkDir;
}
