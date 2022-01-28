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

  /**
   * Determine a format string to display a floating point value with a
   * reasonable number of columns
   */
  private static String floatFmtString(float value) {

    // Normalize values to be nonnegative, but add an extra column for the integer part if necessary

    int log10 = 1;
    if (value != 0) {
      float f10 = (float) Math.log10(Math.abs(value));
      if (f10 > 0)
        log10 = 1 + (int) f10;
      else
        log10 = (int) f10;
      pr("f10:", f10, "log10:", log10);
    }

    /**
     * <pre>
      * 
      * 0           |0.0|
      * 123.4567    |123.46| 
      * -123.4567   |-123.46|
      * 0.001379    |0.00138|
      * -0.001379   |-0.00138|
     * 
     * </pre>
     */

    int MIN_COLUMNS = 3;

    int total, frac;

    if (log10 <= 0) {
      frac = (-log10) + MIN_COLUMNS - 1;
      total = frac + 2;
    } else {
      if (log10 >= MIN_COLUMNS) {
        frac = 0;
        total = log10;
      } else {
        frac = MIN_COLUMNS - log10;
        total = log10 + 1 + frac;
      }
    }
    if (value < 0) {
      total += 1;
    }
    return "%" + total + "." + frac + "f";
  }

  /**
   * Determine a format string to display a statistic using a reasonable number
   * of columns, without scientific notation
   * 
   * @param vMin
   *          minimum value of statistic
   * @param vMax
   *          maximum value of statistic
   */
  private static String floatFmtString(float vMin, float vMax) {

    // Choose as a critical value the maximum of the absolute valus of the min and max stat values
    float vAbsMax = Math.max(Math.abs(vMin), Math.abs(vMax));

    // If we may need to display negative values, negate this critical value
    if (vMin < 0)
      vAbsMax = -vAbsMax;

    float criticalValue = vAbsMax;

    int log10 = 1;
    if (criticalValue != 0) {
      float f10 = (float) Math.log10(Math.abs(criticalValue));
      if (f10 > 0)
        log10 = 1 + (int) f10;
      else
        log10 = (int) f10;
    }

    int MIN_COLUMNS = 3;

    int totalColumns, fractionColumns;

    if (log10 <= 0) {
      fractionColumns = (-log10) + MIN_COLUMNS - 1;
      totalColumns = fractionColumns + 2;
    } else if (log10 >= MIN_COLUMNS) {
      fractionColumns = 0;
      totalColumns = log10;
    } else {
      fractionColumns = MIN_COLUMNS - log10;
      totalColumns = log10 + 1 + fractionColumns;
    }

    // Add an extra column for '-' if it might be required
    //
    if (criticalValue < 0)
      totalColumns += 1;

    return "%" + totalColumns + "." + fractionColumns + "f";
  }

  @Override
  public void perform() {
    float[] va = { 0, 123.4567f, 999.99f, 0.1f, 0.157f, 0.001379f, 1000f, 10000f, 100000f, 0.0001f, 0.0999f };
    for (float v : va) {
      for (int pass = 0; pass < 2; pass++) {
        //        if (pass != 0)
        //          continue;
        float w = (pass == 0) ? v : -v;
        pr(VERT_SP, "value:", w);
        String f = floatFmtString(w);
        pr("format:", f);
        pr("|" + String.format(f, w) + "|");
      }
    }

    float w = -0.2f;
    String f = floatFmtString(-0.03f, 12.2f);
    pr("format:", f);
    pr("|" + String.format(f, w) + "|");

    w = 12.1f;
    pr("|" + String.format(f, w) + "|");

    f = floatFmtString(0f, 0f);
    w = 0f;
    pr("|" + String.format(f, w) + "|");

  }

}
