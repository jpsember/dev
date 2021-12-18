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
import js.file.DirWalk;
import js.file.Files;
import js.graphics.ImgUtil;
import js.graphics.ScriptUtil;
import js.graphics.gen.Script;
import js.json.JSMap;

public class ExtractUsefulScriptsOper extends AppOper {

  @Override
  public String userCommand() {
    return "extractuseful";
  }

  @Override
  public String getHelpDescription() {
    return "extract useful scripts";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[source_dir] [target_dir]");
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
      case 1:
        mTargetDir = Files.absolute(new File(arg));
        break;
      default:
        throw badArg("extraneous argument:", arg);
      }
      count++;
    }
    args.assertArgsDone();

    if (mSourceDir == null)
      mSourceDir = Files.currentDirectory();
    if (mTargetDir == null) {
      mTargetDir = Files.getDesktopFile("_useful_" + mSourceDir.getName());
    }
    if (mTargetDir.exists())
      setError("Target directory already exists:", mTargetDir);
  }

  private File mSourceDir;
  private File mTargetDir;

  @Override
  public void perform() {

    JSMap logMap = map();

    File targetScripts = ScriptUtil.scriptDirForProject(mTargetDir);
    files().mkdirs(targetScripts);
    if (verbose()) {
      logMap.put("target_dir", mTargetDir.getPath());
      logMap.put("source_dir", mSourceDir.getPath());
    }

    DirWalk w = new DirWalk(mSourceDir).withExtensions(ImgUtil.IMAGE_EXTENSIONS);

    int inputImages = w.files().size();
    int outputCount = 0;

    for (File f : w.files()) {
      File scriptPath = ScriptUtil.scriptPathForImage(f);
      if (!scriptPath.exists())
        continue;
      Script script = ScriptUtil.from(scriptPath);
      if (!ScriptUtil.isUseful(script))
        continue;

      files().copyFile(f, new File(mTargetDir, f.getName()));
      files().copyFile(scriptPath, new File(targetScripts, scriptPath.getName()));
      outputCount++;
    }

    if (verbose()) {
      logMap.put("input_count", inputImages) //
          .put("output_count", outputCount);
      pr(logMap);
    }
  }

}
