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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.DirWalk;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.ImgEffects;
import js.graphics.ImgUtil;

public class ResizeOper extends AppOper {

  @Override
  public String userCommand() {
    return "resize";
  }

  @Override
  public String getHelpDescription() {
    return "resize images";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("<dir>");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (args.hasNextArg()) {
      File path = new File(args.nextArg());
      if (!path.isAbsolute()) {
        path = new File(Files.currentDirectory(), path.toString());
      }
      mSourceDir = Files.assertDirectoryExists(path);
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    IPoint targSize = IPoint.with(1200, 1600);
    File targetDir = Files.getDesktopFile("_resize_out_");
    files().mkdirs(targetDir);
    for (File f : new DirWalk(mSourceDir).withRecurse(false).withExtensions(ImgUtil.IMAGE_EXTENSIONS)
        .files()) {
      BufferedImage img = ImgUtil.read(f);
      IPoint src = ImgUtil.size(img);

      BufferedImage result = img;

      float scl = Math.min(targSize.x / (float) src.x, targSize.y / (float) src.y);
      if (scl < 1) {
        result = ImgEffects.scale(img, scl);
        result = ImgUtil.imageAsType(result, BufferedImage.TYPE_3BYTE_BGR);
      }
      // The color doesn't add much, ~1 %
      //
      result = ImgEffects.makeMonochrome3Channel(result);

      File targFile = new File(targetDir, Files.setExtension(f.getName(), ImgUtil.EXT_JPEG));
      byte[] jpg = ImgUtil.toJPEG(result, null, 0.78f);
      files().write(jpg, targFile);
    }
  }

  private File mSourceDir;

}
