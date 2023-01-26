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

import dev.gen.CaptureConfig;
import js.app.AppOper;
import js.base.DateTimeTools;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;

public class ScreenCaptureOper extends AppOper {

  @Override
  public String userCommand() {
    return "screencap";
  }

  @Override
  public String getHelpDescription() {
    return "take screen captures every n seconds";
  }

  @Override
  public CaptureConfig defaultArgs() {
    return CaptureConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {

    mConfig = (CaptureConfig.Builder) config().toBuilder();

    // See https://markholloway.com/2018/11/14/macos-screencapture-terminal/

    while (true) {

      SystemCall s = new SystemCall();
      s.arg("screencapture");
      s.arg("-S"); // Capture the entire screen
      s.arg("-T", 1); // delay in seconds
      //     s.arg("-x");  // Do not play sounds
      s.arg("-r"); // Do not add some metadata to image
      s.arg("-tjpg"); // output image format
      s.arg("-D" + mConfig.device());

      File output = getNextOutputFile();
      log("capturing image to:", output);
      s.arg(output);
      s.setVerbose();
      s.call();
      s.assertSuccess();
      imageFiles().add(output);
      DateTimeTools.sleepForRealMs(mConfig.secondsBetweenShots() * 1000L);

      cullShots();
    }
  }

  private void cullShots() {
    while (imageFiles().size() > mConfig.maxScreenshots()) {
      File x = imageFiles().get(0);
      files().deletePeacefully(x);
      imageFiles().remove(0);
    }
  }

  private List<File> imageFiles() {
    if (mImageFiles == null) {
      List<File> lst = arrayList();
      DirWalk w = new DirWalk(imageDir());
      w.withRecurse(false);
      w.withExtensions("jpg");
      for (File f : w.files()) {
        String name = f.getName();
        if (!name.startsWith(mConfig.imagePrefix()))
          continue;
        lst.add(f);
      }
      mImageFiles = lst;
    }
    return mImageFiles;
  }

  private File imageDir() {
    if (mImageDir == null) {
      File c = mConfig.imageDirectory();
      if (Files.empty(c))
        c = new File(Files.homeDirectory(), "Downloads");
      checkState(c.isDirectory(), "can't find directory:", c);
      mImageDir = c;
    }
    return mImageDir;
  }

  private File mImageDir;

  private File getNextOutputFile() {
    File f = new File(imageDir(), mConfig.imagePrefix() + System.currentTimeMillis() + ".jpg");
    return f;
  }

  private List<File> mImageFiles;
  private CaptureConfig.Builder mConfig;
}
