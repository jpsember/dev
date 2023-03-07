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
import java.net.Socket;

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
      String server = "en.wikipedia.org";
      String path = "/wiki/Main_Page";
      
      // Connect to the server
      Socket socket = new Socket( server, 80 );

      // Create input and output streams to read from and write to the server
      PrintStream out = new PrintStream( socket.getOutputStream() );
      BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

      // Follow the HTTP protocol of GET <path> HTTP/1.0 followed by an empty line
      out.println( "GET " + path + " HTTP/1.0" );
      out.println();

      // Read data from the server until we finish reading the document
      String line = in.readLine();
      while( line != null )
      {
          System.out.println( line );
          line = in.readLine();
      }

      // Close our streams
      in.close();
      out.close();
      socket.close();
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }
}
