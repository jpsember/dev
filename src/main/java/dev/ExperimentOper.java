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

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import dev.gen.ExperimentConfig;
import js.app.AppOper;

public class ExperimentOper extends AppOper {

  @Override
  public String userCommand() {
    loadTools();
    return "exp";
  }

  @Override
  public String getHelpDescription() {
    return "quick experiment";
  }

  @Override
  public ExperimentConfig defaultArgs() {
    return ExperimentConfig.DEFAULT_INSTANCE;
  }

  private static final String MESSAGE = "Hello, World!";

  @Override
  public void perform() {

    try {

      // AES : Advanced Encryption Standard
      //
      // CBC : Cipher Block Chaining
      //
      // PKCS5Padding : pads data so its length is a multiple of 8 bytes
      //

      // Derived from:
      //  https://github.com/luke-park/SecureCompatibleEncryptionExamples/tree/master

      byte[] inputBytes = MESSAGE.getBytes();
      String password = "thatwaseasy";

      byte[] encrypted = encrypt(inputBytes, password);
      byte[] output = decrypt(encrypted, password);
      pr("input :", MESSAGE);
      pr("password:", password);
      pr("encrypted:", INDENT, toSourceCode(encrypted));
      pr("output:", new String(output));
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private final static String ALGORITHM_NAME = "AES/GCM/NoPadding";
  private final static int ALGORITHM_NONCE_SIZE = 12;
  private final static int ALGORITHM_TAG_SIZE = 128;
  private final static int ALGORITHM_KEY_SIZE = 128;
  private final static String PBKDF2_NAME = "PBKDF2WithHmacSHA256";
  private final static int PBKDF2_SALT_SIZE = 16;
  private final static int PBKDF2_ITERATIONS = 32767;

  private static byte[] encrypt(byte[] plainText, String password) throws NoSuchAlgorithmException,
      InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException,
      NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
    // Generate a 128-bit salt using a CSPRNG.
    SecureRandom rand = new SecureRandom();
    byte[] salt = new byte[PBKDF2_SALT_SIZE];
    rand.nextBytes(salt);

    // Create an instance of PBKDF2 and derive a key.
    PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, ALGORITHM_KEY_SIZE);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PBKDF2_NAME);
    byte[] key = keyFactory.generateSecret(pwSpec).getEncoded();

    // Encrypt and prepend salt.
    byte[] ciphertextAndNonce = encrypt(plainText, key);
    byte[] ciphertextAndNonceAndSalt = new byte[salt.length + ciphertextAndNonce.length];
    System.arraycopy(salt, 0, ciphertextAndNonceAndSalt, 0, salt.length);
    System.arraycopy(ciphertextAndNonce, 0, ciphertextAndNonceAndSalt, salt.length,
        ciphertextAndNonce.length);

    return ciphertextAndNonceAndSalt;
  }

  private static byte[] encrypt(byte[] plaintext, byte[] key)
      throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
      NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
    // Generate a 96-bit nonce using a CSPRNG.
    SecureRandom rand = new SecureRandom();
    byte[] nonce = new byte[ALGORITHM_NONCE_SIZE];
    rand.nextBytes(nonce);

    // Create the cipher instance and initialize.
    Cipher cipher = Cipher.getInstance(ALGORITHM_NAME);
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(ALGORITHM_TAG_SIZE, nonce));

    // Encrypt and prepend nonce.
    byte[] ciphertext = cipher.doFinal(plaintext);
    byte[] ciphertextAndNonce = new byte[nonce.length + ciphertext.length];
    System.arraycopy(nonce, 0, ciphertextAndNonce, 0, nonce.length);
    System.arraycopy(ciphertext, 0, ciphertextAndNonce, nonce.length, ciphertext.length);

    return ciphertextAndNonce;
  }

  public static byte[] decrypt(byte[] ciphertextAndNonceAndSalt, String password)
      throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
      InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException,
      NoSuchPaddingException {

    // Retrieve the salt and ciphertextAndNonce.
    byte[] salt = new byte[PBKDF2_SALT_SIZE];
    byte[] ciphertextAndNonce = new byte[ciphertextAndNonceAndSalt.length - PBKDF2_SALT_SIZE];
    System.arraycopy(ciphertextAndNonceAndSalt, 0, salt, 0, salt.length);
    System.arraycopy(ciphertextAndNonceAndSalt, salt.length, ciphertextAndNonce, 0,
        ciphertextAndNonce.length);

    // Create an instance of PBKDF2 and derive the key.
    PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, ALGORITHM_KEY_SIZE);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PBKDF2_NAME);
    byte[] key = keyFactory.generateSecret(pwSpec).getEncoded();

    // Decrypt and return result.
    return decrypt(ciphertextAndNonce, key);
  }

  public static byte[] decrypt(byte[] ciphertextAndNonce, byte[] key)
      throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
      BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {

    // Retrieve the nonce and ciphertext.
    byte[] nonce = new byte[ALGORITHM_NONCE_SIZE];
    byte[] ciphertext = new byte[ciphertextAndNonce.length - ALGORITHM_NONCE_SIZE];
    System.arraycopy(ciphertextAndNonce, 0, nonce, 0, nonce.length);
    System.arraycopy(ciphertextAndNonce, nonce.length, ciphertext, 0, ciphertext.length);

    // Create the cipher instance and initialize.
    Cipher cipher = Cipher.getInstance(ALGORITHM_NAME);
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(ALGORITHM_TAG_SIZE, nonce));

    // Decrypt and return result.
    return cipher.doFinal(ciphertext);
  }

  public static String toSourceCode(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    int i = INIT_INDEX;
    for (byte b : bytes) {
      i++;
      if ((i + 1) % 16 == 0) {
        sb.append('\n');
      }
      sb.append(((int) b) & 0xff);
      sb.append(',');
    }
    return sb.toString();
  }

}
