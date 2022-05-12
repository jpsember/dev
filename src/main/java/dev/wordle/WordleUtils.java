package dev.wordle;

import static js.base.Tools.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import dev.gen.wordle.Dictionary;
import js.data.IntArray;

public final class WordleUtils {

  public static final int WORD_LENGTH = 5;

  public static final int MATCH_NONE = 0;
  public static final int MATCH_PARTIAL = 1;
  public static final int MATCH_FULL = 2;

  public static final int COMPARE_CODE_FINISHED = ((MATCH_FULL << 0) //
      | (MATCH_FULL << 2) //
      | (MATCH_FULL << 4) //
      | (MATCH_FULL << 6) //
      | (MATCH_FULL << 8) //
  );
  public static final int COMPARE_CODE_MAX = COMPARE_CODE_FINISHED + 1;

  private static String vn(String prefix, int i) {
    return prefix + "_" + i;
  }

  private static StringBuilder sGen;

  private static void a(Object... items) {
    for (Object itm : items) {
      sGen.append(' ');
      sGen.append(itm);
    }
  }

  private static void cr() {
    sGen.append('\n');
  }

  public static void genCode() {
    sGen = new StringBuilder();

    a("// Optimized compare for word length:", WORD_LENGTH);
    cr();
    a("final byte N= (byte)MATCH_NONE;");
    cr();
    a("final byte P= (byte)MATCH_PARTIAL;");
    cr();
    a("final byte F= (byte)MATCH_FULL;");
    cr();

    for (int i = 0; i < WORD_LENGTH; i++) {
      a("byte", vn("g", i), "=guessBytes[guessOffset+", i, "];");
      cr();

    }
    for (int i = 0; i < WORD_LENGTH; i++) {
      a("byte", vn("a", i), "=answerBytes[answerOffset+", i, "];");
      cr();
    }
    a("// First pass");
    cr();
    for (int x = 0; x < WORD_LENGTH; x++) {
      a("if (", vn("g", x), "==", vn("a", x), ") {", vn("g", x), "=F;", vn("a", x), "=N;}");
      cr();
    }
    a("// Second pass");
    cr();
    for (int x = 0; x < WORD_LENGTH; x++) {
      for (int y = 0; y < WORD_LENGTH; y++) {
        a("if (", vn("g", x), "==", vn("a", y), "&&", vn("g", x), "!=F) {", vn("g", x), "=P;", vn("a", y),
            "=N;}");
        cr();
      }
      a("if (", vn("g", x), ">F)", vn("g", x), "=N;");
      cr();
    }

    a("return ");
    for (int x = 0; x < WORD_LENGTH; x++) {
      if (x > 0) {
        a(" | ");
      }
      if (x > 0)
        a("(");
      a(vn("g", x));
      if (x > 0)
        a("<<", x * 2);
      if (x > 0)
        a(")");
    }
    a(";");
    cr();

    pr(VERT_SP, sGen, VERT_SP);
  }

  public static int compareOpt(byte[] answerBytes, int answerOffset, byte[] guessBytes, int guessOffset) {

    // Perform two passes
    //
    // First pass:
    //   for each x
    //     if guess[x] == answer[x]:
    //        set guess[x] = GREEN, answer[x] = GRAY
    //   
    // Second pass:
    //   for each x
    //     for each y
    //        if guess[x] == answer[y] and guess[x] != GREEN
    //           set guess[x] = YELLOW, answer[y] = GRAY
    //     if guess[x] != GREEN and guess[x] != YELLOW
    //           set guess[x] = GRAY
    //
    // Return guess[] values encoded as compare code
    //

    // Optimized compare for word length: 5
    final byte N = (byte) MATCH_NONE;
    final byte P = (byte) MATCH_PARTIAL;
    final byte F = (byte) MATCH_FULL;
    byte g_0 = guessBytes[guessOffset + 0];
    byte g_1 = guessBytes[guessOffset + 1];
    byte g_2 = guessBytes[guessOffset + 2];
    byte g_3 = guessBytes[guessOffset + 3];
    byte g_4 = guessBytes[guessOffset + 4];
    byte a_0 = answerBytes[answerOffset + 0];
    byte a_1 = answerBytes[answerOffset + 1];
    byte a_2 = answerBytes[answerOffset + 2];
    byte a_3 = answerBytes[answerOffset + 3];
    byte a_4 = answerBytes[answerOffset + 4];
    // First pass
    if (g_0 == a_0) {
      g_0 = F;
      a_0 = N;
    }
    if (g_1 == a_1) {
      g_1 = F;
      a_1 = N;
    }
    if (g_2 == a_2) {
      g_2 = F;
      a_2 = N;
    }
    if (g_3 == a_3) {
      g_3 = F;
      a_3 = N;
    }
    if (g_4 == a_4) {
      g_4 = F;
      a_4 = N;
    }
    // Second pass
    if (g_0 == a_0 && g_0 != F) {
      g_0 = P;
      a_0 = N;
    }
    if (g_0 == a_1 && g_0 != F) {
      g_0 = P;
      a_1 = N;
    }
    if (g_0 == a_2 && g_0 != F) {
      g_0 = P;
      a_2 = N;
    }
    if (g_0 == a_3 && g_0 != F) {
      g_0 = P;
      a_3 = N;
    }
    if (g_0 == a_4 && g_0 != F) {
      g_0 = P;
      a_4 = N;
    }
    if (g_0 > F)
      g_0 = N;
    if (g_1 == a_0 && g_1 != F) {
      g_1 = P;
      a_0 = N;
    }
    if (g_1 == a_1 && g_1 != F) {
      g_1 = P;
      a_1 = N;
    }
    if (g_1 == a_2 && g_1 != F) {
      g_1 = P;
      a_2 = N;
    }
    if (g_1 == a_3 && g_1 != F) {
      g_1 = P;
      a_3 = N;
    }
    if (g_1 == a_4 && g_1 != F) {
      g_1 = P;
      a_4 = N;
    }
    if (g_1 > F)
      g_1 = N;
    if (g_2 == a_0 && g_2 != F) {
      g_2 = P;
      a_0 = N;
    }
    if (g_2 == a_1 && g_2 != F) {
      g_2 = P;
      a_1 = N;
    }
    if (g_2 == a_2 && g_2 != F) {
      g_2 = P;
      a_2 = N;
    }
    if (g_2 == a_3 && g_2 != F) {
      g_2 = P;
      a_3 = N;
    }
    if (g_2 == a_4 && g_2 != F) {
      g_2 = P;
      a_4 = N;
    }
    if (g_2 > F)
      g_2 = N;
    if (g_3 == a_0 && g_3 != F) {
      g_3 = P;
      a_0 = N;
    }
    if (g_3 == a_1 && g_3 != F) {
      g_3 = P;
      a_1 = N;
    }
    if (g_3 == a_2 && g_3 != F) {
      g_3 = P;
      a_2 = N;
    }
    if (g_3 == a_3 && g_3 != F) {
      g_3 = P;
      a_3 = N;
    }
    if (g_3 == a_4 && g_3 != F) {
      g_3 = P;
      a_4 = N;
    }
    if (g_3 > F)
      g_3 = N;
    if (g_4 == a_0 && g_4 != F) {
      g_4 = P;
      a_0 = N;
    }
    if (g_4 == a_1 && g_4 != F) {
      g_4 = P;
      a_1 = N;
    }
    if (g_4 == a_2 && g_4 != F) {
      g_4 = P;
      a_2 = N;
    }
    if (g_4 == a_3 && g_4 != F) {
      g_4 = P;
      a_3 = N;
    }
    if (g_4 == a_4 && g_4 != F) {
      g_4 = P;
      a_4 = N;
    }
    if (g_4 > F)
      g_4 = N;
    return g_0 | (g_1 << 2) | (g_2 << 4) | (g_3 << 6) | (g_4 << 8);
  }

  public static int compare(Word answerWord, Word guessWord) {
    return compareOpt(answerWord.lettersArray(), answerWord.lettersStart(), guessWord.lettersArray(),
        guessWord.lettersStart());
  }

  public static String compareCodeString(int compareCode) {
    String colorChars = ".◯●";
    StringBuilder sb = new StringBuilder("(");
    for (int i = 0; i < WORD_LENGTH; i++) {
      int c = (compareCode >> (i * 2)) & 3;
      sb.append(colorChars.charAt(c));
    }
    sb.append(')');
    return sb.toString();
  }

  public static String renderMatch(Word query, int compareCode) {
    String queryText = query.toString();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < WORD_LENGTH; i++) {
      char c = queryText.charAt(i);
      sb.append(c);
      switch (compareCode & 3) {
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
      compareCode >>= 2;
    }

    return sb.toString();
  }

  public static short[] buildCompareCodeFrequencyTable() {
    return new short[COMPARE_CODE_MAX];
  }

  public static void clearCompareCodeFreqTable(short[] compareCodeFreq) {
    Arrays.fill(compareCodeFreq, (short) 0);
  }

  /**
   * Given a set of possible answers, determine the best guesses
   */
  public static int[] bestGuess(WordSet dict) {
    int dictSize = dict.size();
    byte[] dictWords = WordSet.defaultDictEntry().dictionary.wordBytes();

    short[] compareCodeFreq = buildCompareCodeFrequencyTable();

    // 
    // Let q be a word in the dictionary.
    //
    // Comparing q to all the words in the dictionary partitions the dictionary into subsets,
    // each of which has the property that each target t in the subset produces an identical result 
    // when comparing q and t.
    //
    // Choose the q* that the largest subset {t0, t1, ...} is as small as possible.
    //

    IntArray.Builder bestQuerys = IntArray.newBuilder();

    int minMaxCompareCodeFreq = Integer.MAX_VALUE;

    // Heuristic:  we don't need to look at every word in the dictionary as a guess candidate,
    // if the dictionary is large. Instead, choose a random sample of the words
    //
    int maxWords = dictSize;
    if (dictSize > 2000) {
      maxWords = (int) (Math.sqrt(dictSize - 2000) + 2000);
    }
    int[] samples = new int[maxWords];
    if (maxWords == dictSize) {
      for (int i = 0; i < maxWords; i++)
        samples[i] = i;
    } else {
      Random r = new Random();
      for (int i = 0; i < maxWords; i++)
        samples[i] = r.nextInt(dictSize);
    }

    for (int queryIndex : samples) {
      int guessWordId = dict.wordId(queryIndex);

      clearCompareCodeFreqTable(compareCodeFreq);

      for (int answerIndex = 0; answerIndex < dictSize; answerIndex++) {
        int answerWordId = dict.wordId(answerIndex);
        int result = compareOpt(dictWords, answerWordId, dictWords, guessWordId);
        compareCodeFreq[result]++;
      }

      // Choose the worst case, the largest subset
      //

      int mostFrequentCode = maxCompareCodeFreq(compareCodeFreq);
      int frequency = compareCodeFreq[mostFrequentCode];
      if (frequency < minMaxCompareCodeFreq) {
        minMaxCompareCodeFreq = frequency;
        bestQuerys.clear();
      }
      if (frequency == minMaxCompareCodeFreq)
        bestQuerys.add(guessWordId);
    }

    return bestQuerys.array();
  }

  private static int maxCompareCodeFreq(short[] compareCodeFreq) {
    short maxFrequency = -1;
    int maxIndex = -1;

    int index = INIT_INDEX;
    for (short freq : compareCodeFreq) {
      index++;
      if (freq > maxFrequency) {
        maxFrequency = freq;
        maxIndex = index;
      }
    }
    return maxIndex;
  }

  public static List<String> sortWordsForDisplay(List<String> words) {
    List<String> common = arrayList();
    List<String> rare = arrayList();
    Dictionary comDict = WordSet.smallDictionary().dictionary;
    for (String w : words) {
      if (dictContainsWord(comDict, w))
        common.add(w);
      else
        rare.add(w);
    }
    common.addAll(rare);
    return common;
  }

  public static boolean dictContainsWord(Dictionary dictionary, String word) {
    return Collections.binarySearch(dictionary.words(), word.toUpperCase()) >= 0;
  }

  public static String formatWords(List<String> words) {
    StringBuilder sb = new StringBuilder();
    int i = INIT_INDEX;
    for (String w : words) {
      i++;
      sb.append(w);
      sb.append((i % 8) == 7 ? '\n' : ' ');
    }
    return sb.toString().toLowerCase();
  }

}
