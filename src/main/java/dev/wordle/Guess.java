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

import static js.base.Tools.*;
import static dev.wordle.WordleUtils.*;

public class Guess {

  public static Guess with(Word word, int compareCode) {
    Guess g = new Guess();
    g.mWord = word;
    g.mCompareCode = compareCode;
    return g;
  }

  public static Guess parse(String s) {

    try {
      StringBuilder w = new StringBuilder();
      int cres = 0;

      s = s.trim().toUpperCase();
      int i = 0;
      while (i < s.length()) {
        char c = s.charAt(i++);
        if (!(c >= 'A' && c <= 'Z'))
          badArg();
        int pos = w.length();
        w.append(c);

        int match = MATCH_NONE;
        if (i < s.length()) {
          c = s.charAt(i);
          if (c == '*')
            match = MATCH_FULL;
          else if (c == '\'')
            match = MATCH_PARTIAL;
          if (match != MATCH_NONE)
            i++;
        }
        cres |= (match << (2 * pos));
      }

      if (w.length() != WORD_LENGTH)
        badArg();
      return with(new Word(w.toString()), cres);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Word word() {
    return mWord;
  }

  public int compareCode() {
    return mCompareCode;
  }

  private Word mWord;
  private int mCompareCode;
}
