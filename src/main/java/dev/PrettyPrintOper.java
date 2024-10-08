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

import java.io.File;
import java.util.List;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.BasePrinter;
import js.file.Files;
import js.json.JSMap;

public class PrettyPrintOper extends AppOper {

  @Override
  public String userCommand() {
    return "pretty";
  }

  @Override
  public String shortHelp() {
    return "pretty-print json files";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("[overwrite] <file>+");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();

    mOverwrite = args.nextArgIf("overwrite");

    if (args.hasNextArg()) {
      File relPath = new File(args.nextArg());
      if (!relPath.isAbsolute()) {
        relPath = new File(Files.currentDirectory(), relPath.toString());
      }
      mFiles.add(relPath);
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    if (mFiles.isEmpty())
      pr("(please specify one or more json files)");
    for (File f : mFiles) {
      if (!f.exists())
        f = Files.addExtension(f, Files.EXT_JSON);
      var m = JSMap.from(f);
      if (mOverwrite) {
        files().writeIfChanged(f, m.prettyPrint());
      } else
        pr(m);
    }
  }

  private List<File> mFiles = arrayList();
  private boolean mOverwrite;
}
