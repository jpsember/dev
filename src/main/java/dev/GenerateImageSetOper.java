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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import dev.gen.GenerateImagesConfig;
import js.app.AppOper;
import js.data.DataUtil;
import js.data.IntArray;
import js.file.DirWalk;
import js.file.Files;
import js.geometry.Matrix;
import js.geometry.MyMath;
import js.graphics.ImgUtil;
import js.graphics.Paint;
import js.graphics.Plotter;

public class GenerateImageSetOper extends AppOper {

  @Override
  public String userCommand() {
    return "genimages";
  }

  @Override
  public String getHelpDescription() {
    return "Generate some images for experiment purposes";
  }

  private List<Paint> paints() {
    if (mColors == null) {
      int[] sc = sColors;
      if (config().monochrome())
        sc = sColorsMono;
      mColors = arrayList();
      for (int i = 0; i < sc.length; i += 3) {
        mColors.add(Paint.newBuilder().color(sc[i], sc[i + 1], sc[i + 2]).build());
      }
    }
    return mColors;
  }

  private Paint PAINT_BGND = Paint.newBuilder().color(220, 220, 220).build();

  private <T> T randomElement(List<T> elements) {
    return elements.get(random().nextInt(elements.size()));
  }

  @Override
  public void perform() {
    files().mkdirs(config().targetDir());
    for (File f : new DirWalk(config().targetDir()).withExtensions("jpg", "bin").files()) {
      files().deleteFile(f);
    }

    String categoriesString = config().categories();

    IntArray.Builder categories = IntArray.newBuilder();

    OutputStream imageStream = null;
    if (config().mergeImages()) {
      checkArgument(config().writeFloats() && config().writeUncompressed());
      imageStream = files().outputStream(new File(config().targetDir(), "images.bin"));
    }

    for (int i = 0; i < config().imageTotal(); i++) {

      Plotter p = Plotter.build();
      p.withCanvas(config().imageSize());

      FontInfo fi = randomElement(fonts());
      p.with(PAINT_BGND).fillRect();
      p.with(randomElement(paints()).toBuilder().font(fi.mFont, 1f));

      int category = random().nextInt(categoriesString.length());
      String text = categoriesString.substring(category, category + 1);
      categories.add(category);

      FontMetrics m = fi.metrics(p.graphics());

      int mx = config().imageSize().x / 2;
      int my = config().imageSize().y / 2;

      float rangex = mx * config().translateFactor();
      float rangey = my * config().translateFactor();

      Matrix tfmFontOrigin = Matrix.getTranslate(-m.charWidth(categoriesString.charAt(0)) / 2,
          m.getAscent() / 2);
      Matrix tfmImageCenter = Matrix.getTranslate(randGuassian(mx - rangex, mx + rangex),
          randGuassian(my - rangey, my + rangey));
      Matrix tfmRotate = Matrix
          .getRotate(randGuassian(-config().rotFactor() * MyMath.M_DEG, config().rotFactor() * MyMath.M_DEG));
      Matrix tfmScale = Matrix.getScale(randGuassian(config().scaleFactorMin(), config().scaleFactorMax()));

      Matrix tfm = Matrix.postMultiply(tfmImageCenter, tfmScale, tfmRotate, tfmFontOrigin);

      p.graphics().setTransform(tfm.toAffineTransform());
      p.graphics().drawString(text, 0, 0);

      if (imageStream != null) {
        float[] pixels = ImgUtil.floatPixels(p.image(), config().monochrome() ? 1 : 3, null);
        files().writeFloatsLittleEndian(pixels, imageStream);
      } else {
        String path = String.format("image_%05d.jpg", i);
        File f = new File(config().targetDir(), path);

        if (config().writeUncompressed()) {
          f = Files.setExtension(f, "bin");
          if (config().writeFloats()) {
            float[] pixels = ImgUtil.floatPixels(p.image(), config().monochrome() ? 1 : 3, null);
            files().writeFloatsLittleEndian(pixels, f);
          } else {
            if (config().monochrome())
              setError("monochrome not supported for integer pixels (yet)");
            BufferedImage bgrImage = ImgUtil.imageAsType(p.image(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] pix = ((DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();
            files().write(pix, f);
          }
        } else
          ImgUtil.writeImage(files(), p.image(), f);
      }
    }
    Files.close(imageStream);

    byte[] categoryBytes = DataUtil.intsToBytesLittleEndian(categories.array());
    files().write(categoryBytes, new File(config().targetDir(), "labels.bin"));
  }

  private float randGuassian(float min, float max) {
    float scl = (max - min) * 0.35f;
    float center = (max + min) * 0.5f;
    while (true) {
      float g = (float) (random().nextGaussian() * scl + center);
      if (g >= min && g <= max)
        return g;
    }
  }

  @Override
  public GenerateImagesConfig defaultArgs() {
    return GenerateImagesConfig.DEFAULT_INSTANCE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public GenerateImagesConfig config() {
    return super.config();
  }

  private class FontInfo {
    Font mFont;
    FontMetrics mMetrics;

    public FontMetrics metrics() {
      checkState(mMetrics != null);
      return mMetrics;
    }

    public FontMetrics metrics(Graphics2D g) {
      if (mMetrics == null)
        mMetrics = g.getFontMetrics(mFont);
      return metrics();
    }
  }

  private void addFont(String family) {
    FontInfo fi = new FontInfo();
    fi.mFont = new Font(family, Font.PLAIN, 12);
    fonts().add(fi);

    fi = new FontInfo();
    fi.mFont = new Font(family, Font.BOLD, 12);
    fonts().add(fi);
  }

  private List<FontInfo> fonts() {
    if (mFonts == null) {
      mFonts = arrayList();
      addFont("Dialog");
      addFont("DialogInput");
      addFont("Monospaced");
      addFont("Serif");
      addFont("SansSerif");
    }
    return mFonts;
  }

  private Random random() {
    if (mRandom == null)
      mRandom = new Random(config().seed());
    return mRandom;
  }

  private int[] sColors = { //
      74, 168, 50, //
      50, 107, 168, //
      168, 101, 50, //
  };
  private int[] sColorsMono = { //
      100, 100, 100, //
      80, 80, 80, //
      40, 40, 40, //
      20, 20, 20, //
  };

  private Random mRandom;
  private List<FontInfo> mFonts;
  private List<Paint> mColors;

}