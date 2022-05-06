package dev.wordle;

import static js.base.Tools.*;
import static dev.wordle.WordleUtils.*;

public class Guess {

  public static Guess with(Word word, int compareResult) {
    Guess g = new Guess();
    g.mWord = word;
    g.mCompareResult = compareResult;
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
      return with(Word.with(w.toString()), cres);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Word word() {
    return mWord;
  }

  public int compareResult() {
    return mCompareResult;
  }

  private Word mWord;
  private int mCompareResult;
}
