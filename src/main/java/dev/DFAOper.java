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

import dev.tokn.DFACompiler;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;
import js.json.JSMap;

public class DFAOper extends AppOper {

  @Override
  public String userCommand() {
    return "dfa";
  }

  @Override
  public String getHelpDescription() {
    return "compile ." + OBJECT_EXT + " file from an ." + SOURCE_EXT + " file";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("<." + SOURCE_EXT + " input> [<." + OBJECT_EXT + " output>]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    for (int i = 0; i < 2; i++) {
      if (!args.hasNextArg())
        break;
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
      pr("(please specify an ." + SOURCE_EXT + " files)");
    File sourceFile = mFiles.get(0);
    File targetFile = null;
    if (mFiles.size() >= 2)
      targetFile = mFiles.get(1);
    processSourceFile(sourceFile, targetFile);
  }

  private static final String SOURCE_EXT = "rxp";
  private static final String OBJECT_EXT = "dfa";

  private void processSourceFile(File sourceFile, File targetFile) {
    sourceFile = assertExt(Files.addExtension(sourceFile, SOURCE_EXT), SOURCE_EXT);

    if (!sourceFile.exists())
      setError("No such file:", sourceFile);

    if (Files.empty(targetFile))
      targetFile = Files.setExtension(sourceFile, OBJECT_EXT);
    assertExt(targetFile, OBJECT_EXT);

    DFACompiler compiler = new DFACompiler();
    compiler.setVerbose(verbose());
    JSMap jsonMap = compiler.parse(Files.readString(sourceFile));
    String str = jsonMap.toString();
    files().writeIfChanged(targetFile, str);
  }

  private File assertExt(File file, String ext) {
    if (!Files.getExtension(file).equals(ext))
      setError("Not a ." + ext + " file:", file);
    return file;
  }

  private List<File> mFiles = arrayList();

}
