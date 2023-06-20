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
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

      // Derived from: https://www.baeldung.com/java-aes-encryption-decryption
      byte[] input = MESSAGE.getBytes();
      SecretKey key = generateKey(128);
      
      IvParameterSpec ivParameterSpec = generateIv();
      String algorithm = "AES/CBC/PKCS5Padding";
      byte[] encrypted = encryptBytes(algorithm, input, key, ivParameterSpec);

      byte[] otherKey = { 109, 121, 51, 50, 100, 105, 103, 105, 116, 107, 101, 121, 49, 50, 51, 52, //
          53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48 };
      byte[] otherIv = { 109, 121, 49, 54, 100, 105, 103, 105, 116, 73, 118, 75, 101, 121, 49, 50 };

      IvParameterSpec ivsp = ivParameterSpec;
      SecretKey othKey = key;

      if (false) {
        ivsp = new IvParameterSpec(otherIv);
        othKey = new SecretKeySpec(otherKey, "AES");
      }

      byte[] ourKey = new byte[16];
      for (int i = 0; i < ourKey.length; i++) {
        ourKey[i] = (byte) (i * 17);
      }
      othKey = new SecretKeySpec(ourKey, 0, ourKey.length, "AES");

      byte[] output = decryptBytes(algorithm, encrypted, othKey, ivsp);
      pr("input :", MESSAGE);
      pr("output:", new String(output));
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private static SecretKey generateKey(int n) {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(n);
      SecretKey key = keyGenerator.generateKey();
      return key;
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private static IvParameterSpec generateIv() {
    byte[] iv = new byte[16];
    new SecureRandom().nextBytes(iv);
    return new IvParameterSpec(iv);
  }

  private static byte[] encryptBytes(String algorithm, byte[] input, SecretKey key, IvParameterSpec iv) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      return cipher.doFinal(input);
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private static byte[] decryptBytes(String algorithm, byte[] encrypted, SecretKey key, IvParameterSpec iv) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      return cipher.doFinal(encrypted);
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }
}
