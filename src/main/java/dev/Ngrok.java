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
      loadTools();
      sSharedInstance = new Ngrok();
      todo(
          "we need to support a 'secrets' directory where we can store things like the ngrok password, something not tracked by .git");
      todo(
          "we need to support a 'project_config' directory where we can store non-secret things like cached remote system information");
    }
    return sSharedInstance;
  }

  private static Ngrok sSharedInstance;

  /**
   * Find a file within a project directory structure, by ascending to a parent
   * directory until found (or we run out of parents)
   * 
   * @param startParentDirectoryOrNull
   *          directory to start search within, or null for current directory
   * @param filename
   *          name of file (or directory) to look for
   * @return found file, or null
   */
  public static File locateFileWithinParents(File startParentDirectoryOrNull, String filename) {
    todo("move this to a utility class");
    File dir;
    if (startParentDirectoryOrNull == null)
      dir = Files.currentDirectory();
    else
      dir = Files.absolute(startParentDirectoryOrNull);
    while (true) {
      File candidate = new File(dir, filename);
      pr("candidate:", Files.infoMap(candidate));
      if (candidate.exists())
        return candidate;
      dir = dir.getParentFile();
      if (dir == null)
        return null;
    }
  }

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
    JSMap result = new JSMap(sc.systemOut());
    return result;
  }

  /**
   * Temporary, for testing
   */
  public JSMap tunnels() {
    return callAPI("tunnels");
  }

  private String getNgrokToken() {
    if (mToken == null) {
      File secretsDir = locateFileWithinParents(null, "secrets");
      checkState(!Files.empty(secretsDir), "can't locate secrets dir");
      File tokenFile = new File(secretsDir, "ngrok_token.txt");
      checkState(tokenFile.exists(), "no such file:", tokenFile);
      mToken = Files.readString(tokenFile).trim();
    }
    return mToken;
  }

  private String mToken;
}
