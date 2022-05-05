package dev.wordle;

import static js.base.Tools.*;
import static dev.wordle.WordleUtils.*;

public class Guess {

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
      Guess g = new Guess();
      g.mWord = w.toString();
      g.mCompareResult = cres;
      return g;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public String word() {
    return mWord;
  }

  public int compareResult() {
    return mCompareResult;
  }

  private String mWord;
  private int mCompareResult;
}
