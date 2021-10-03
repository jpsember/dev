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
import java.util.List;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;

public class SetupMachineOper extends AppOper {

  @Override
  public String userCommand() {
    return "setup";
  }

  @Override
  public String getHelpDescription() {
    return "setup system";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return null;
    //return arrayList("[<xxx>]", "[<yyy>]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (false) {
      if (args.hasNextArg()) {
      }
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    validateOS();
    prepareSSH();
  }

  private void validateOS() {
    String osName = System.getProperty("os.name", "<none>");
    if (osName.equals("<none>") || osName.contains("OS X"))
      throw badState("Unexpected os.name:", osName, "(are you running this on your Mac by mistake?");
  }

  private void prepareSSH() {
    File sshDir = new File("~/.ssh");
    pr("sshDir:",Files.infoMap(sshDir));
  }
  
}
