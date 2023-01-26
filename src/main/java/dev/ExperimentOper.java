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

import dev.gen.ExperimentConfig;
import js.app.AppOper;

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
  public ExperimentConfig defaultArgs() {
    return ExperimentConfig.DEFAULT_INSTANCE;
  }


  @Override
  public void perform() {

    halt("nothing here");
//    
//    // See https://markholloway.com/2018/11/14/macos-screencapture-terminal/
//
//    while (true) {
//
//      SystemCall s = new SystemCall();
//      s.arg("screencapture");
//      s.arg("-S"); // Capture the entire screen
//      s.arg("-T", 1); // delay in seconds
//      //     s.arg("-x");  // Do not play sounds
//      s.arg("-r"); // Do not add some metadata to image
//      s.arg("-tjpg"); // output image format
//      s.arg("-D" + mDeviceNumber);
//
//      File output = getNextOutputFile();
//      log("capturing image to:", output);
//      s.arg(output);
//      s.setVerbose();
//      s.call();
//      s.assertSuccess();
//      imageFiles().add(output);
//      final int sleepSeconds = 3;
//      DateTimeTools.sleepForRealMs(sleepSeconds * 1000L);
//
//      if (imageFiles().size() > 3)
//        break;
//    }
  }

//  private List<File> imageFiles() {
//    if (mImageFiles == null) {
//      List<File> lst = arrayList();
//      DirWalk w = new DirWalk(imageDir());
//      w.withRecurse(false);
//      w.withExtensions("jpg");
//      for (File f : w.files()) {
//        String name = f.getName();
//        if (!name.startsWith(prefix))
//          continue;
//        lst.add(f);
//      }
//      mImageFiles = lst;
//    }
//    return mImageFiles;
//  }
//
//  private static final String prefix = "_screencapture_";
//
//  private File imageDir() {
//    if (mImageDir == null) {
//      mImageDir = new File(Files.homeDirectory(), "Downloads");
//      checkState(mImageDir.isDirectory(), "can't find directory:", mImageDir);
//    }
//    return mImageDir;
//  }
//
//  private File mImageDir;
//
//  private File getNextOutputFile() {
//    File f = new File(imageDir(), prefix + System.currentTimeMillis() + ".jpg");
//    return f;
//  }
//
//  private List<File> mImageFiles;
//  private int mDeviceNumber = 1;
}
