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
import dev.gen.FetchCloudConfig;
import js.app.AppOper;
import js.json.JSMap;

public class FetchCloudFilesOper extends AppOper {

  @Override
  public String userCommand() {
    return "fetchcloud";
  }

  @Override
  public String getHelpDescription() {
    return "read files from S3";
  }

  @Override
  public void perform() {
    FetchCloudConfig config = config();
    checkArgument(!config.subfolderPath().isEmpty(), "subfolder_path must not be empty");
    
    File authFile = files().fileWithinSecrets("s3_auth.json");
    JSMap m = JSMap.from(authFile);
    String profile = m.get("profile");
    S3Archive archive = new S3Archive(profile, m.get("account_name"), config.subfolderPath(), null);
    archive.setVerbose(verbose());
    ArchiveDevice device = archive;

    List<CloudFileEntry> entries = device.listFiles(null);
    log("found", entries.size(), "files");

    int fetched = 0;
    for (CloudFileEntry ent : entries) {
      if (fetched >= config.maxFetchCount())
        break;
      File dest = new File(ent.name());
      if (dest.exists())
        continue;
      todo("have a stats file that keeps track of most recent file fetched to skip it and any earlier ones");
      fetched++;
      log("fetching #", fetched, INDENT, ent);
      device.pull(ent.name(), dest);
    }
  }

  @Override
  public FetchCloudConfig defaultArgs() {
    return FetchCloudConfig.DEFAULT_INSTANCE;
  }

}
