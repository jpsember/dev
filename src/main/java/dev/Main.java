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

import dev.cmit.CmitOper;
import dev.installer.MakeInstallerOper;
import js.app.App;

public class Main extends App {

  public static final String VERSION = "1.0";

  public static void main(String[] args) {
    loadTools();
    App app = new Main();
    //app.setCustomArgs("-v --exceptions getrepo latest");
    app.startApplication(args);
    app.exitWithReturnCode();
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  protected void registerOperations() {
    registerOper(new CreateAppOper());
    registerOper(new ResetTestOper());
    registerOper(new CopyrightOper());
    registerOper(new ExperimentOper());
    registerOper(new PushOper());
    registerOper(new PullOper());
    registerOper(new SetupMachineOper());
    registerOper(new SecretsOper());
    registerOper(new ArchiveOper());
    registerOper(new PrettyPrintOper());
    registerOper(new ResizeOper());
    registerOper(new RemoveExtraneousScriptElementsOper());
    registerOper(new ConvertJsonOper());
    registerOper(new FetchCloudFilesOper());
    registerOper(new MakeScriptOper());
    registerOper(new MakeInstallerOper());
    registerOper(new NewOperOper());
    registerOper(new NewDatOper());
    registerOper(new InstallOper());
    registerOper(new UninstallOper());
    registerOper(new GetRepoOper());
    registerOper(new PrepOper());
    registerOper(new CmitOper());
    // --- insertion point for new operations (used by NewOperOper)
  }

}
