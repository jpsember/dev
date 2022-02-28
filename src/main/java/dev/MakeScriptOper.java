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

public class MakeScriptOper extends AppOper {

  @Override
  public String userCommand() {
    return "mkscript";
  }

  @Override
  public String getHelpDescription() {
    return "Create an executable script";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("(bash | python)* <script_name>");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();

    int argNum = INIT_INDEX;
    outer: while (args.hasNextArg()) {
      argNum++;
      String arg = args.peekNextArg();
      switch (argNum) {
      default:
        break outer;
      case 0: {
        switch (arg) {
        case "bash":
          mType = ScriptType.BASH;
          args.nextArg();
          break;
        case "python":
          mType = ScriptType.PYTHON;
          args.nextArg();
          break;
        }
      }
        break;

      case 1:
        String ext = null;
        switch (mType) {
        default:
          throw notSupported();
        case BASH:
          ext = "sh";
          break;
        case PYTHON:
          ext = "py";
          break;
        }
        mTargetFile = Files.setExtension(new File(args.nextArg()), ext);
      }
    }
    args.assertArgsDone();
  }

  enum ScriptType {
    BASH, PYTHON,
  };

  @Override
  public void perform() {
    StringBuilder sb = new StringBuilder();
    switch (mType) {
    default:
      throw notSupported();
    case BASH: {
      sb.append("#!/usr/bin/env bash\n");
      sb.append("set -eu\n");
      sb.append("\n");
    }
      break;
    case PYTHON: {
      sb.append("#!/usr/bin/env python3\n");
      sb.append("\n");
    }
      break;
    }
    files().writeString(mTargetFile, sb.toString());
    files().chmod(mTargetFile, 744);
  }

  private ScriptType mType = ScriptType.BASH;
  private File mTargetFile;

}
