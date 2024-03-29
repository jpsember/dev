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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.BasePrinter;
import js.file.DirWalk;
import js.file.Files;
import js.webtools.RemoteManager;
import js.webtools.gen.RemoteEntityInfo;

public class SecretsOper extends AppOper {

  @Override
  public String userCommand() {
    return "secrets";
  }

  @Override
  public String shortHelp() {
    return "decrypt (or encrypt) secrets";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("[encrypt] passphrase <text> [entity <id>]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    while (args.hasNextArg()) {
      String arg = args.nextArg();
      switch (arg) {
      case "encrypt":
        mEncryptMode = true;
        break;
      case "passphrase":
        mPassPhrase = args.nextArg();
        break;
      case "entity":
        mEntityId = args.nextArg();
        break;
      default:
        throw badArg("extraneous argument:", arg);
      }
    }
    args.assertArgsDone();
  }

  /**
   * Encrypt bytes using passphrase and AES encryption
   */
  public static byte[] encryptData(String passPhrase, byte[] data) {
    try {
      SecureRandom secureRandom = new SecureRandom();
      byte[] nonce = new byte[12];
      secureRandom.nextBytes(nonce);
      SecretKey secretKey = generateSecretKey(passPhrase, nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonce);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
      byte[] encryptedData = cipher.doFinal(data);
      ByteBuffer byteBuffer = ByteBuffer.allocate(4 + nonce.length + encryptedData.length);
      byteBuffer.putInt(nonce.length);
      byteBuffer.put(nonce);
      byteBuffer.put(encryptedData);
      return byteBuffer.array();
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  /**
   * Decrypt bytes that were encrypted via encryptData()
   */
  public static byte[] decryptData(String passPhrase, byte[] data) {
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(data);
      int nonceSize = byteBuffer.getInt();
      checkArgument(nonceSize >= 12 && nonceSize < 16, "bad nonce size");
      byte[] nonce = new byte[nonceSize];
      byteBuffer.get(nonce);
      SecretKey secretKey = generateSecretKey(passPhrase, nonce);
      byte[] cipherBytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherBytes);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonce);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
      return cipher.doFinal(cipherBytes);
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private static SecretKey generateSecretKey(String passphrase, byte[] nonce) {
    // Remove whitespace from passphrase, and convert to lower case
    passphrase = passphrase.replaceAll("\\s", "").toLowerCase();
    checkArgument(passphrase.length() >= 8, "passphrase is too short");
    try {
      KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), nonce, 65536, 128); // AES-128
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();
      return new SecretKeySpec(key, "AES");
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private byte[] zipDirectory(File directory, String... omitFileList) {
    Set<File> omitSet = hashSet();
    for (String omitFile : omitFileList)
      omitSet.add(new File(omitFile));
    try {
      DirWalk dirWalk = new DirWalk(directory);
      ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
      ZipOutputStream zipStream = new ZipOutputStream(byteArrayStream);
      for (File relFile : dirWalk.filesRelative()) {
        if (omitSet.contains(relFile))
          continue;
        String relPath = relFile.toString();
        ZipEntry zipEntry = new ZipEntry(relPath);
        zipStream.putNextEntry(zipEntry);
        zipStream.write(Files.toByteArray(new File(dirWalk.directory(), relPath), "SecretsOper.2"));
        zipStream.closeEntry();
      }
      zipStream.close();
      return byteArrayStream.toByteArray();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  private void unzipDirectory(byte[] zippedBytes, File targetDir) {
    if (targetDir.exists()) {
      files().backupManager().backupAndDelete(targetDir);
    }
    files().mkdirs(targetDir);
    try {
      ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes));
      byte[] buffer = new byte[2048];
      ZipEntry zipEntry;
      while ((zipEntry = zipStream.getNextEntry()) != null) {
        String relativePath = zipEntry.getName();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = zipStream.read(buffer)) > 0) {
          outputStream.write(buffer, 0, length);
        }
        byte[] content = outputStream.toByteArray();
        File targetFile = new File(targetDir, relativePath);
        files().mkdirs(Files.parent(targetFile));
        files().write(content, targetFile);
      }
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  @Override
  public void perform() {
    checkArgument(!nullOrEmpty(mPassPhrase), "Please provide a passphrase");

    // This is made more complicated due to
    //
    // 1) the entity_info.json file is stored in the secrets directory, and is not 
    //    to be included in the encrypted directory contents
    // 2) this operation is also (optionally) used to create this entity_info.json
    //    file when decrypting the secrets

    if (mEncryptMode) {

      // Encrypt all the files in the local project secrets directory, 
      // except for `entity_info.json` which varies for each entity

      byte[] zipFileBytes = zipDirectory(files().projectSecretsDirectory(), Files.SECRETS_FILE_ENTITY_INFO);
      byte[] encrypted = encryptData(mPassPhrase, zipFileBytes);
      File target = files().fileWithinProjectConfigDirectory("encrypted_secrets.bin");
      files().write(encrypted, target);

    } else {

      // Using the passphrase, decrypt the secrets into this entity's secrets directory

      File secretsDir = files().optFileWithinProject("secrets");

      // If there is an entity id argument, write the entity's info as well; otherwise,
      // leave that file untouched (if it existed)

      File entityInfoFile = new File(secretsDir, Files.SECRETS_FILE_ENTITY_INFO);
      String currentEntityInfoContent = Files.readString(entityInfoFile, "");

      boolean updateEntityInfo = nonEmpty(mEntityId);
      RemoteEntityInfo entityInfo = null;
      if (updateEntityInfo) {
        var mgr = RemoteManager.SHARED_INSTANCE;
        var ent = mgr.activeEntity();
        checkState(mEntityId.equals(ent.label()), "expected entity to be the active one");
      }

      byte[] encrypted = Files.toByteArray(files().fileWithinProjectConfigDirectory("encrypted_secrets.bin"),
          "SecretsOper.1");
      byte[] decrypted = decryptData(mPassPhrase, encrypted);
      unzipDirectory(decrypted, secretsDir);

      // If we are to update the entity info, do so from the entity manager; otherwise, restore
      // the content we saved earlier (if there was any)

      if (updateEntityInfo) {
        files().writePretty(entityInfoFile, entityInfo);
      } else {
        if (nonEmpty(currentEntityInfoContent)) {
          files().writeString(entityInfoFile, currentEntityInfoContent);
        }
      }
    }
  }

  private boolean mEncryptMode;
  private String mPassPhrase;
  private String mEntityId;

}
