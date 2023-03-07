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

import java.io.ByteArrayOutputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dev.gen.ExperimentConfig;
import js.app.AppOper;
import js.base.DateTimeTools;

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

  @Override
  public void perform() {
    try {
      listFolderStructure();
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private void listFolderStructure() throws Exception {

    String username = "pi";
    String host = "4.tcp.ngrok.io";
    int port = 15034;

    Session session = null;
    ChannelExec channel = null;

    try {
      JSch jsch = new JSch();

      String privateKey = "/Users/home/.ssh/id_rsa";

      jsch.addIdentity(privateKey);
      pr("identity added ");

      session = jsch.getSession(username, host, port);
      pr("session created");

      session.setConfig("StrictHostKeyChecking", "no");

      session.connect();
      pr("connected");

      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand("ls");
      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
      channel.setOutputStream(responseStream);
      channel.connect();

      while (channel.isConnected()) {
        DateTimeTools.sleepForRealMs(100);
      }

      String responseString = new String(responseStream.toByteArray());
      System.out.println(responseString);
    } finally {
      if (session != null) {
        session.disconnect();
      }
      if (channel != null) {
        channel.disconnect();
      }
    }
  }
}
