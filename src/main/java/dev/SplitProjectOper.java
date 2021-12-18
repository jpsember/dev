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
import js.base.Pair;
import js.file.Files;
import js.graphics.ScriptUtil;
import js.json.JSList;
import js.json.JSMap;

public class SplitProjectOper extends AppOper {

  @Override
  public String userCommand() {
    return "split-project";
  }

  @Override
  public String getHelpDescription() {
    return "split project into smaller sub-projects";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[source_dir] [max_size]");
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
        mMaxSize = Integer.parseInt(arg);
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

    JSMap logMap = map();

    if (verbose()) {
      logMap//
          .put("source_dir", mSourceDir.getPath()) //
          .put("max_size", mMaxSize) //
      ;
    }

    File sourceScriptsDir = ScriptUtil.scriptDirForProject(mSourceDir);

    List<Pair<String, String>> scriptList = ScriptUtil.buildScriptList(mSourceDir);

    logMap.put("script_count", scriptList.size());

    int inputImages = scriptList.size();
    int nchunks = (inputImages + mMaxSize - 1) / mMaxSize;

    if (nchunks > 1) {
      JSList chunks = list();
      logMap.put("chunks", chunks);

      for (int chunkIndex = 0; chunkIndex < nchunks; chunkIndex++) {
        int cursor = chunkIndex * mMaxSize;
        int chunkSize = Math.min(mMaxSize, inputImages - cursor);

        String suffix;
        if (nchunks <= 26)
          suffix = "" + (char) ('a' + chunkIndex);
        else
          suffix = String.format("%02d", chunkIndex);
        String targetProjectName = Files.basename(mSourceDir) + "_" + suffix;

        File targetProjectDir = new File(Files.parent(mSourceDir), targetProjectName);
        checkState(!targetProjectDir.exists(), "File already exists:", INDENT,
            Files.infoMap(targetProjectDir));
        File targetScriptsDir = files().mkdirs(ScriptUtil.scriptDirForProject(targetProjectDir));
        for (int j = 0; j < chunkSize; j++) {
          Pair<String, String> entry = scriptList.get(cursor + j);
          String imageName = entry.first;
          String scriptName = entry.second;
          if (nonEmpty(imageName)) {
            File imageFile = new File(mSourceDir, imageName);
            files().copyFile(imageFile, new File(targetProjectDir, imageName));
          }
          {
            File scriptFile = new File(sourceScriptsDir, scriptName);
            if (scriptFile.exists())
              files().copyFile(scriptFile, new File(targetScriptsDir, imageName));
          }
        }
        chunks.add(map().put("dir", targetProjectDir.toString()).put("count", chunkSize));
      }
    }

    if (verbose()) {
      pr(logMap);
    }
  }

  private File mSourceDir;
  private int mMaxSize = 500;

}
