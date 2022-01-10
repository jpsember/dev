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

import dev.gen.CloudFileEntry;
import dev.gen.ExperimentConfig;
import js.app.AppOper;
import js.file.Files;
import js.json.JSMap;

public class ExperimentOper extends AppOper {

  @Override
  public String userCommand() {
    loadTools();
    return "exp";
  }

  @Override
  public String getHelpDescription() {
    return "quick experiment";
  }

  @Override
  public void perform() {

    File projectDirectory = Files.getCanonicalFile(Files.parent(files().projectConfigDirectory()));

    File authFile = files().fileWithinSecrets("s3_auth.json");
    JSMap m = JSMap.from(authFile);

    String profile = m.get("profile");
    if (false && alert("using bogus profile"))
      profile = "android_app";
    S3Archive archive = new S3Archive(profile, m.get("account_name"), "cloud_writer", projectDirectory);
    archive.setVerbose(verbose());
    ArchiveDevice device = archive;

    if (true) {
      List<CloudFileEntry> entries = device.listFiles(null);
      pr(entries);
      return;
    }

    if (true) {
      boolean exists = device.fileExists("experiment_NONE.txt");
      pr("exists:", exists);
      return;
    }

    if (true) {
      File sample = Files.getDesktopFile("experiment_read.txt");
      pr("attempting to pull:", sample);
      device.pull("experiment.txt", sample);
      pr("read:", Files.infoMap(sample));
      return;
    }

    if (true) {
      File sample = Files.getDesktopFile("experiment.txt");
      files().writeString(sample, "hello");
      pr("attempting to push:", sample);
      device.push(sample, "experiment.txt");
      return;
    }

    List<CloudFileEntry> entries = device.listFiles(null);
    pr(entries);
  }

  @Override
  public ExperimentConfig defaultArgs() {
    return ExperimentConfig.DEFAULT_INSTANCE;
  }

}
