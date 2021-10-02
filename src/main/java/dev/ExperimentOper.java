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

import dev.gen.ExperimentConfig;
import js.app.AppOper;
import js.file.Files;

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
  public void perform() {
    Ngrok ng = Ngrok.sharedInstance();
    pr("ngrok:", INDENT, ng.toJson());
    pr("find:", Files.getFileWithinParents(new File("src"), ".git"));
    pr("tunnels", INDENT, ng.tunnels());
    pr("tunnel_sessions", INDENT, ng.tunnelSessions());
    pr("entity map:", INDENT, EntityManager.sharedInstance().entities());
  }

  @Override
  public ExperimentConfig defaultArgs() {
    return ExperimentConfig.DEFAULT_INSTANCE;
  }

}
