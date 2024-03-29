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

import js.file.Files;
import js.webtools.gen.RemoteEntityInfo;

public final class Utils {

  public static RemoteEntityInfo ourEntityInfo() {
    if (sRemoteEntityInfo == null) {
      loadTools();
      sRemoteEntityInfo = RemoteEntityInfo.DEFAULT_INSTANCE.parse(Files.S.entityInfo());
    }
    return sRemoteEntityInfo;
  }

  private static RemoteEntityInfo sRemoteEntityInfo;

  /**
   * <pre>
   * 
   * Notes specific to OSX configuration
   * -----------------------------------
   * 
   * + disabling strange messages in Terminal when ending a session:
   *    Create this file in home directory (https://stackoverflow.com/questions/32418438):
   *       touch ~/.bash_sessions_disable
   * 
   *    Note, this disables more functionality related to bash sessions introduced in El Capitan.
   * 
   * 
   */
}
