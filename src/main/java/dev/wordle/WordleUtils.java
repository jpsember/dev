package dev.wordle;

import static js.base.Tools.*;

public final class WordleUtils {

  public static final int WORD_LENGTH = 5;

  private static final int CODE_SKIP = '.';

  private static final int MATCH_NONE = 0;
  private static final int MATCH_PARTIAL = 1;
  private static final int MATCH_FULL = 2;

  public static Word word(String text) {
    return new Word(text);
  }

  public static int compare(Word target, Word query) {
    byte[] matchCodes = sWork;
    clearWork(matchCodes);

    byte[] targCopy = sWork2;
    target.readLetters(targCopy);
    byte[] quer = query.letters();

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (quer[i] == targCopy[i]) {
        matchCodes[i] = MATCH_FULL;
        targCopy[i] = CODE_SKIP;
      }
    }

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (matchCodes[i] != MATCH_NONE)
        continue;
      int q = quer[i];
      for (int j = 0; j < WORD_LENGTH; j++) {
        if (targCopy[j] == CODE_SKIP)
          continue;
        if (targCopy[j] == q) {
          matchCodes[i] = MATCH_PARTIAL;
          targCopy[j] = CODE_SKIP;
          break;
        }
      }
    }

    int mc = matchCodes[0] //
        | (matchCodes[1] << 2 * 1) //
        | (matchCodes[2] << 2 * 2) //
        | (matchCodes[3] << 2 * 3) //
        | (matchCodes[4] << 2 * 4) //
    ;
    return mc;
  }

  public static String renderMatch(Word query, int match) {
    String queryText = query.toString();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < WORD_LENGTH; i++) {
      char c = queryText.charAt(i);
      sb.append(c);
      switch (match & 3) {
      default: // MATCH_NONE
        sb.append(' ');
        break;
      case MATCH_PARTIAL:
        sb.append('\'');
        break;
      case MATCH_FULL:
        sb.append('*');
        break;
      }
      match >>= 2;
      sb.append(' ');
    }
    return sb.toString();
  }

  static {
    loadTools();
  }

  private static final void clearWork(byte[] a) {
    for (int i = 0; i < WORD_LENGTH; i++)
      a[i] = 0;
  }

  private static final byte[] sWork = new byte[WORD_LENGTH];
  private static final byte[] sWork2 = new byte[WORD_LENGTH];

}
