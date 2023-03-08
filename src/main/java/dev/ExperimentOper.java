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
import com.jcraft.jsch.JSchException;
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

      checkpoint("opening JSch");
      JSch jsch = new JSch();

      String privateKey = "/Users/home/.ssh/issue40d";

      // The key pair must be generated with this command:
      //
      //   ssh-keygen -m PEM
      //
      try {
        jsch.addIdentity(privateKey);
      } catch (JSchException e) {
        String msg = e.getMessage();
        if (msg.contains("invalid privatekey")) {
          pr("*** Failed to add identity to JSch; was the key created via 'ssh-keygen -m PEM' ?");
        }
        throw e;
      }
      pr("identity added ");

      checkpoint("getting session");
      session = jsch.getSession(username, host, port);

      session.setConfig("StrictHostKeyChecking", "no");

      checkpoint("connecting");

      session.connect();
      checkpoint("connected");

      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

      pr("looping for several times");
      for (int j = 0; j < 10; j++) {
        responseStream.reset();
        checkpoint("opening channel, iter", j);
        channel = (ChannelExec) session.openChannel("exec");
        channel.setOutputStream(responseStream);
        channel.setCommand("ls -1 BarnServ-Alpha1/source/barnserv/start/data");
        channel.connect();

        while (channel.isConnected()) {
          DateTimeTools.sleepForRealMs(50);
        }

        String responseString = new String(responseStream.toByteArray()).trim();

        pr(responseString);

        int status = channel.getExitStatus();
        checkpoint("finished command, status:", status);
      }
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
