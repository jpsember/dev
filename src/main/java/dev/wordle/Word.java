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
package dev.wordle;

import static dev.wordle.WordleUtils.*;
import static js.base.Tools.*;

public final class Word {

  public Word(String word) {
    if (word.length() != WORD_LENGTH)
      badArg("wrong length for word:", quote(word));
    mLetters = new byte[WORD_LENGTH];
    word = word.toUpperCase();
    for (int i = 0; i < WORD_LENGTH; i++)
      mLetters[i] = (byte) word.charAt(i);
  }

  public byte[] lettersArray() {
    return mLetters;
  }

  public int lettersStart() {
    return mLettersStart;
  }

  public Word(byte[] bytes, int id) {
    set(bytes, id);
  }

  private void set(byte[] sourceArray, int id) {
    mLetters = sourceArray;
    mLettersStart = id;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(WORD_LENGTH);
    for (int i = 0; i < WORD_LENGTH; i++)
      sb.append((char) mLetters[i + mLettersStart]);
    return sb.toString();
  }

  private byte[] mLetters;
  private int mLettersStart;

}
