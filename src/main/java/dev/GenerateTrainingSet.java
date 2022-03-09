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
import java.io.OutputStream;

import dev.gen.AnnotationFile;
import dev.gen.GenerateTrainingSetConfig;
import js.app.AppOper;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.ImgUtil;

public class GenerateTrainingSet extends AppOper {

  @Override
  public String userCommand() {
    return "gentrainset";
  }

  @Override
  public String getHelpDescription() {
    return "Generate set of training images";
  }

  private File targetDir() {
    if (mTargetDir == null) {
      File t = Files.assertNonEmpty(config().targetDir(), "target_dir");
      mTargetDir = files().mkdirs(t);
    }
    return mTargetDir;
  }

  private File mTargetDir;

  @Override
  public void perform() {
    OutputStream imageStream = files().outputStream(new File(targetDir(), "images.bin"));

    AnnotationFile af = Files.parseAbstractData(AnnotationFile.DEFAULT_INSTANCE,
        Files.assertDirectoryExists(config().sourceImageInfo(), "source_image_info"));
    File srcImages = Files.assertDirectoryExists(config().sourceImagesDir(), "source_images_dir");

    int index = INIT_INDEX;
    for (String filename : af.filenames()) {
      index++;

      File srcImageFile = new File(srcImages, filename);
      BufferedImage srcImage = ImgUtil.read(srcImageFile);
      IPoint size = ImgUtil.size(srcImage);
      checkArgument(size.equals(config().imageSize()), "unexpected image size");

      todo("add image to output");
      todo("add annotations to output");

      //      if (imageStream != null) {
      //        float[] pixels = ImgUtil.floatPixels(p.image(), config().monochrome() ? 1 : 3, null);
      //        files().writeFloatsLittleEndian(pixels, imageStream);
      //      } else {
      //        String path = String.format("image_%05d.jpg", i);
      //        File f = new File(config().targetDir(), path);
      //
      //        if (config().writeUncompressed()) {
      //          f = Files.setExtension(f, "bin");
      //          if (config().writeFloats()) {
      //            float[] pixels = ImgUtil.floatPixels(p.image(), config().monochrome() ? 1 : 3, null);
      //            files().writeFloatsLittleEndian(pixels, f);
      //          } else {
      //            if (config().monochrome())
      //              setError("monochrome not supported for integer pixels (yet)");
      //            BufferedImage bgrImage = ImgUtil.imageAsType(p.image(), BufferedImage.TYPE_3BYTE_BGR);
      //            byte[] pix = ((DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();
      //            files().write(pix, f);
      //          }
      //        } else
      //          ImgUtil.writeImage(files(), p.image(), f);
      //      }
    }
    Files.close(imageStream);
    //
    //    byte[] categoryBytes = DataUtil.intsToBytesLittleEndian(categories.array());
    //    files().write(categoryBytes, new File(config().targetDir(), "labels.bin"));
  }

  @Override
  public GenerateTrainingSetConfig defaultArgs() {
    return GenerateTrainingSetConfig.DEFAULT_INSTANCE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public GenerateTrainingSetConfig config() {
    return super.config();
  }

}
