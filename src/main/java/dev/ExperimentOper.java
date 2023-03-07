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

import dev.gen.ExperimentConfig;
import js.app.AppOper;
import js.json.JSList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class ExperimentOper extends AppOper {

  @Override
  public String userCommand() {
    loadTools();
    return "exp";
  }

  @Override
  public String getHelpDescription() {
    return "quick experiment: open ssl socket connection to remote machine";
  }

  @Override
  public ExperimentConfig defaultArgs() {
    return ExperimentConfig.DEFAULT_INSTANCE;
  }

  private static final String[] protocols = new String[] { "TLSv1.2" };
  private static final String[] cipher_suites = new String[] { "TLS_AES_128_GCM_SHA256" };

  @Override
  public void perform() {
    try {

      SSLSocket socket = null;
      PrintWriter out = null;
      BufferedReader in = null;

      try {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = (SSLSocket) factory.createSocket("google.com", 443);

        // Get the list of all supported cipher suites.
        String[] cipherSuites = socket.getSupportedCipherSuites();
        pr(JSList.with(cipherSuites));
        halt();

        socket.setEnabledProtocols(protocols);
        socket.setEnabledCipherSuites(cipher_suites);

        socket.startHandshake();

        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));

        out.println("GET / HTTP/1.0");
        out.println();
        out.flush();

        if (out.checkError())
          System.out.println("SSLSocketClient:  java.io.PrintWriter error");

        /* read response */
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
          System.out.println(inputLine);

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (socket != null)
          socket.close();
        if (out != null)
          out.close();
        if (in != null)
          in.close();
      }

    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }
}
