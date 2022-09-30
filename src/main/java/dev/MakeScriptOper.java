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
import js.json.JSList;
import js.json.JSMap;

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
    return arrayList("<script_name[.ext]>");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    String path = args.nextArg();
    mExt = ifNullOrEmpty(Files.getExtension(path), "sh");
    mTargetFile = new File(Files.setExtension(path, mExt));
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    JSList template = template().optJSList(mExt);
    if (template == null)
      throw setError("Unsupported extension:", mExt);
    String content = String.join("\n", template.asStrings()) + "\n\n";
    files().writeString(mTargetFile, content);
    files().chmod(mTargetFile, 744);
  }

  private JSMap template() {
    if (mTemplate == null)
      mTemplate = JSMap.fromResource(this.getClass(), "makescript.json");
    return mTemplate;
  }

  private JSMap mTemplate;
  private String mExt;
  private File mTargetFile;

}
