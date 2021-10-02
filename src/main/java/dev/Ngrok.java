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

import js.base.BaseObject;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSMap;

public class Ngrok extends BaseObject {

  public static Ngrok sharedInstance() {
    if (sSharedInstance == null) {
      sSharedInstance = new Ngrok();
    }
    return sSharedInstance;
  }

  private static Ngrok sSharedInstance;

  /**
   * Call the ngrok API, with a particular endpoint appended to their url.
   * 
   * @param endpoint
   * @return JSMap
   */
  private JSMap callAPI(String endpoint) {
    SystemCall sc = new SystemCall();
    //sc.alertVerbose();
    sc.arg("curl", "-sS");
    sc.arg("-H", "Accept: application/json");
    sc.arg("-H", "Authorization: Bearer " + getNgrokToken());
    sc.arg("https://api.ngrok.com/" + endpoint);
    sc.assertSuccess();
    JSMap result = new JSMap(sc.systemOut());
    return result;
  }

  /**
   * Temporary, for testing
   */
  public JSMap tunnels() {
    return callAPI("tunnels");
  }

  /**
   * This doesn't output much; probably a feature we're not using
   */
  public JSMap endpoints() {
    return callAPI("endpoint_configurations");
  }

  public JSMap tunnelSessions() {
    return callAPI("tunnel_sessions");
  }

  private String getNgrokToken() {
    if (mToken == null) {
      File tokenFile = new File(Files.S.projectSecretsDirectory(), "ngrok_token.txt");
      checkState(tokenFile.exists(), "no such file:", tokenFile);
      mToken = Files.readString(tokenFile).trim();
    }
    return mToken;
  }

  private String mToken;

}
