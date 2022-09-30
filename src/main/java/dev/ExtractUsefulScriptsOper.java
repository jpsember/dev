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
import js.file.Files;
import js.graphics.ScriptUtil;
import js.graphics.gen.ScriptFileEntry;
import js.json.JSMap;

public class ExtractUsefulScriptsOper extends AppOper {

  @Override
  public String userCommand() {
    return "trim-project";
  }

  @Override
  public String getHelpDescription() {
    return "delete unused images and scripts";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[source_dir]");
  }

  @Override
  protected void processAdditionalArgs() {
    int count = 0;
    CmdLineArgs args = app().cmdLineArgs();
    while (args.hasNextArg()) {
      String arg = args.nextArg();
      switch (count) {
      case 0:
        mSourceDir = Files.absolute(new File(arg));
        break;

      default:
        throw badArg("extraneous argument:", arg);
      }
      count++;
    }
    args.assertArgsDone();

    if (mSourceDir == null)
      mSourceDir = Files.currentDirectory();
  }

  @Override
  public void perform() {

    JSMap logMap = map().put("source_dir", mSourceDir.getPath());

    List<ScriptFileEntry> scriptEntries = ScriptUtil.buildScriptList(mSourceDir, true);
    File scriptDir = ScriptUtil.scriptDirForProject(mSourceDir);

    int inputImages = scriptEntries.size();
    int outputCount = 0;

    for (ScriptFileEntry ent : scriptEntries) {
      File scriptPath = new File(scriptDir, ent.scriptName());
      if (scriptPath.exists() && ScriptUtil.isUseful(ScriptUtil.from(scriptPath))) {
        outputCount++;
        continue;
      }
      if (scriptPath.exists())
        files().deleteFile(scriptPath);
      if (nonEmpty(ent.imageName()))
        files().deleteFile(new File(mSourceDir, ent.imageName()));
    }

    if (verbose()) {
      logMap.put("input_count", inputImages) //
          .put("output_count", outputCount);
      pr(logMap);
    }
  }

  private File mSourceDir;

}
