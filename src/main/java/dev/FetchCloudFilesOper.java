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

import dev.gen.FetchCloudConfig;
import js.app.AppOper;
import js.json.JSMap;
import js.webtools.ArchiveDevice;
import js.webtools.S3Archive;
import js.webtools.gen.CloudFileEntry;
import js.webtools.gen.S3Params;

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

    ArchiveDevice device;
    {
      File authFile = files().fileWithinSecrets("s3_auth.json");
      JSMap m = JSMap.from(authFile);
      S3Archive archive = new S3Archive(S3Params.newBuilder() //
          .profile(m.get("profile")) //
          .bucketName(m.get("account_name")) //
      );
      archive.setDryRun(dryRun());
      device = archive;
    }

    JSMap stats = stats();
    String mostRecentPath = stats.opt("recent", "");

    List<CloudFileEntry> entries = device.listFiles(config.subfolderPath());
    log("found", entries.size(), "files");

    int fetched = 0;
    for (CloudFileEntry ent : entries) {
      if (fetched >= config.maxFetchCount())
        break;
      log("...name:", ent.name());
      if (ent.name().compareTo(mostRecentPath) <= 0) {
        log("...name <= most recent", mostRecentPath);
        continue;
      }

      File dest = new File(ent.name());
      if (dest.exists()) {
        log("...already exists");
        continue;
      }
      fetched++;
      log("fetching #", fetched, INDENT, ent);
      alert("does it pull directories to the basename of the supplied name?");
      String fullPath;
      if (nonEmpty(config.subfolderPath()))
        fullPath = config.subfolderPath() + "/" + ent.name();
      else
        fullPath = ent.name();

      device.pull(fullPath, dest);

      mostRecentPath = ent.name();
      stats.put("recent", mostRecentPath);
      files().writePretty(statsFile(), stats);
    }
  }

  @Override
  public FetchCloudConfig defaultArgs() {
    return FetchCloudConfig.DEFAULT_INSTANCE;
  }

  private JSMap stats() {
    return JSMap.fromFileIfExists(statsFile());
  }

  private File statsFile() {
    return new File("fetch_cloud_stats.json");
  }

}
