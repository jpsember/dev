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
import java.io.File;
import java.util.List;
import java.util.Random;

import dev.gen.GenerateImagesConfig;
import js.app.AppOper;
import js.file.DirWalk;
import js.geometry.IRect;
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
      mColors = arrayList();
      for (int i = 0; i < sColors.length; i += 3) {
        mColors.add(Paint.newBuilder().color(sColors[i], sColors[i + 1], sColors[i + 2]).build());
      }
    }
    return mColors;
  }

  private Paint PAINT_BGND = Paint.newBuilder().color(192, 192, 192).build();

  private <T> T randomElement(List<T> elements) {
    return elements.get(random().nextInt(elements.size()));
  }

  @Override
  public void perform() {
    files().mkdirs(config().targetDir());
    for (File f : new DirWalk(config().targetDir()).withExtensions("jpg").files()) {
      files().deleteFile(f);
    }
    for (int i = 0; i < config().imageTotal(); i++) {

      Plotter p = Plotter.build();
      p.withCanvas(config().imageSize());

      FontInfo fi = randomElement(fonts());
      p.with(PAINT_BGND).fillRect();
      p.with(randomElement(paints()).toBuilder().font(fi.mFont, 1f));

      IRect b = p.bounds();

      char c = 'H';
      FontMetrics m = fi.metrics(p.graphics());
      int x = b.midX() - m.charWidth(c) / 2;
      int y = b.midY() + m.getAscent() / 2;
      p.graphics().drawString(Character.toString(c), x, y);

      String path = String.format("image_%03d.jpg", i);
      File f = new File(config().targetDir(), path);
      ImgUtil.writeImage(files(), p.image(), f);
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

  private int[] sColors = { 74, 168, 50, //
      50, 107, 168, //
      168, 101, 50, //
  };

  private Random mRandom;
  private List<FontInfo> mFonts;
  private List<Paint> mColors;

}
