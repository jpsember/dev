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
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.json.JSRewrite;

public class ConvertJsonOper extends AppOper {

  @Override
  public String userCommand() {
    return "json-convert";
  }

  @Override
  public String getHelpDescription() {
    return "convert json files from datagen format to standard json";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[ compact | pretty ]* [source_dir [target_dir]]");
  }

  @Override
  protected void processAdditionalArgs() {
    int count = 0;
    CmdLineArgs args = app().cmdLineArgs();
    while (args.hasNextArg()) {
      String arg = args.nextArg();

      switch (arg) {
      case "compact":
        mMode = MODE_COMPACT;
        break;
      case "pretty":
        mMode = MODE_PRETTY;
        break;
      default: {
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
        break;
      }
    }
    args.assertArgsDone();

    if (mSourceDir == null)
      mSourceDir = Files.currentDirectory();
    if (mTargetDir == null)
      mTargetDir = mSourceDir;
  }

  @Override
  public void perform() {

    JSMap logMap = map() //
        .put("source_dir", mSourceDir.getPath()) //
        .put("target_dir", mTargetDir.getPath()) //
    ;

    files().mkdirs(mTargetDir);

    DirWalk w = new DirWalk(mSourceDir).withExtensions(Files.EXT_JSON);

    for (File absFile : w.files()) {
      String sourceText = Files.readString(absFile);
      int targetMode = mMode;
      if (targetMode == MODE_ADAPTIVE)
        targetMode = sourceText.trim().contains("\n") ? MODE_PRETTY : MODE_COMPACT;
      JSMap sourceMap = new JSMap(sourceText);
      JSMap targetMap = (JSMap) JSRewrite.CONVERT_DATA_TO_JSON_REWRITER.rewrite(sourceMap);
      String targetText = (targetMode == MODE_PRETTY) ? targetMap.prettyPrint() : targetMap.toString();
      files().writeString(Files.join(mTargetDir, w.rel(absFile)), targetText);
    }

    if (verbose()) {
      logMap.put("files_processed", w.files().size()) //
      ;
      pr(logMap);
    }
  }

  private static final int MODE_ADAPTIVE = 0;
  private static final int MODE_PRETTY = 1;
  private static final int MODE_COMPACT = 2;

  private File mSourceDir;
  private File mTargetDir;
  private int mMode = MODE_ADAPTIVE;

}
