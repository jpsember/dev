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
import java.util.regex.Matcher;

import dev.gen.RemoteEntityInfo;
import js.base.BaseObject;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;

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
   * Discard any cached tunnels that may have previously been read
   */
  public Ngrok discardTunnels() {
    mCachedTunnels = null;
    return this;
  }

  /**
   * Get ngrok tunnel corresponding to an entity
   * 
   * @param tag
   *          entity's tag
   * @return RemoteEntityInfo containing url and port, or null if no matching
   *         tunnel found
   */
  public RemoteEntityInfo tunnelInfo(String tag) {
    RemoteEntityInfo result = null;
    for (JSMap tunMap : tunnelsMap().asMaps()) {
      String metadata = tunMap.get("metadata");
      if (metadata.isEmpty()) {
        if (alert("until metadata working")) {
          if (tunMap.get("public_url").contains("18995")) {
            metadata = "rpi32";
            tunMap.put("metadata", metadata);
          } else if (tunMap.get("public_url").contains("16890")) {
            metadata = "rpi64";
            tunMap.put("metadata", metadata);
          } else if (tunMap.get("public_url").contains("16881")) {
            metadata = "rpi";
            tunMap.put("metadata", metadata);
          }
        }
      }
      if (metadata.isEmpty()) {
        pr("*** ngrok tunnel has no metadata, public_url:", tunMap.get("public_url"));
        continue;
      }

      if (!metadata.equals(tag))
        continue;

      if (result != null) {
        pr("*** multiple tunnels sharing same metadata:", metadata);
        break;
      }

      String publicUrl = tunMap.get("public_url");
      chompPrefix(publicUrl, "tcp://");
      Matcher matcher = RegExp.matcher("tcp:\\/\\/(.+):(\\d+)", publicUrl);
      if (!matcher.matches()) {
        pr("*** failed to parse public_url:", publicUrl);
        continue;
      }
      result = RemoteEntityInfo.newBuilder()//
          .url(matcher.group(1)) //
          .port(Integer.parseInt(matcher.group(2)))//
          .build();
    }
    return result;
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

  private JSList tunnelsMap() {
    if (mCachedTunnels == null)
      mCachedTunnels = callAPI("tunnels").getList("tunnels");
    return mCachedTunnels;
  }

  private JSList mCachedTunnels;

  private String mToken;

}
