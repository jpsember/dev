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
import js.graphics.PolygonElement;
import js.graphics.ScriptElement;
import js.graphics.ScriptUtil;
import js.graphics.gen.Script;
import js.graphics.gen.ScriptFileEntry;
import js.json.JSMap;

/**
 * For scripts that have a single polygon element, trims any other elements from
 * the script
 */
public class RemoveExtraneousScriptElementsOper extends AppOper {

  @Override
  public String userCommand() {
    return "trim-elements";
  }

  @Override
  public String getHelpDescription() {
    return "remove extraneous ScriptElements from project";
  }

  @Override
  protected void getOperSpecificHelp(BasePrinter b) {
    b.pr("[project_dir]");
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

    JSMap logMap = map() //
        .put("source_dir", mSourceDir.getPath());

    File scriptsDir = ScriptUtil.scriptDirForProject(mSourceDir);

    int scriptsProcessed = 0;
    int scriptsModified = 0;

    for (ScriptFileEntry ent : ScriptUtil.buildScriptList(mSourceDir, true)) {

      File scriptPath = new File(scriptsDir, ent.scriptName());
      if (!scriptPath.exists())
        continue;

      scriptsProcessed++;
      Script script = ScriptUtil.from(scriptPath);
      List<PolygonElement> polys = ScriptUtil.polygonElements(script);
      if (polys.size() > 0) {

        if (polys.size() > 1) {
          pr("*** Multiple polygons found:", ent.scriptName(), INDENT, script);
          continue;
        }

        if (script.items().size() == 1)
          continue;

        List<ScriptElement> modList = arrayList();
        modList.addAll(polys);

        Script modScript = script.toBuilder().items(modList).build();

        if (modScript.equals(script))
          continue;
        if (false)
          log("Modified:", ent.scriptName(), INDENT, script, CR, "New content:", CR, modScript);
        ScriptUtil.write(files(), modScript, scriptPath);
        scriptsModified++;
      }
    }

    if (verbose()) {
      logMap.put("input_count", scriptsProcessed) //
          .put("modified_count", scriptsModified);
      pr(logMap);
    }
  }

  private File mSourceDir;

}
