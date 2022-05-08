package dev.wordle;

import static js.base.Tools.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import dev.gen.wordle.Dictionary;
import js.data.IntArray;

public final class WordleUtils {

  public static final int WORD_LENGTH = 5;

  private static final int CODE_SKIP = '.';

  public static final int MATCH_NONE = 0;
  public static final int MATCH_PARTIAL = 1;
  public static final int MATCH_FULL = 2;

  public static int compare(Word answerWord, Word guessWord) {
    byte[] matchCodes = sWork;
    clearWork(matchCodes);

    byte[] answerCopy = sWork2;
    answerWord.readLetters(answerCopy);
    byte[] guessBytes = guessWord.letters();

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (guessBytes[i] == answerCopy[i]) {
        matchCodes[i] = MATCH_FULL;
        answerCopy[i] = CODE_SKIP;
      }
    }

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (matchCodes[i] != MATCH_NONE)
        continue;
      int q = guessBytes[i];
      for (int j = 0; j < WORD_LENGTH; j++) {
        if (answerCopy[j] == CODE_SKIP)
          continue;
        if (answerCopy[j] == q) {
          matchCodes[i] = MATCH_PARTIAL;
          answerCopy[j] = CODE_SKIP;
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

  private static final void clearWork(byte[] a) {
    for (int i = 0; i < WORD_LENGTH; i++)
      a[i] = 0;
  }

  private static Comparator<WordPartitionSubset> SUBSET_COMPARATOR = new Comparator<WordPartitionSubset>() {
    @Override
    public int compare(WordPartitionSubset o1, WordPartitionSubset o2) {
      int res = o1.pop() - o2.pop();
      if (res == 0)
        res = o1.compareCode() - o2.compareCode();
      return res;
    }
  };

  /**
   * Given a set of possible answers, determine the best guesses
   */
  public static int[] bestGuess(WordSet dict) {

    Map<Integer, WordPartitionSubset> partitionMap = hashMap();
    Word guessWord = Word.buildEmpty();
    Word answerWord = Word.buildEmpty();
    int dictSize = dict.size();

    // 
    // Let q be a word in the dictionary.
    //
    // Comparing q to all the words in the dictionary partitions the dictionary into subsets,
    // each of which has the property that each target t in the subset produces an identical result 
    // when comparing q and t.
    //
    // Choose the q* that the largest subset {t0, t1, ...} is as small as possible.
    //

    WordPartitionSubset bestGlobal = null;
    Map<Integer, WordPartitionSubset> bestPartitionMap = null;

    IntArray.Builder bestQuerys = IntArray.newBuilder();

    for (int queryIndex = 0; queryIndex < dictSize; queryIndex++) {
      dict.getWord(guessWord, queryIndex);

      partitionMap.clear();

      for (int answerIndex = 0; answerIndex < dictSize; answerIndex++) {
        dict.getWord(answerWord, answerIndex);

        int result = compare(answerWord, guessWord);
        WordPartitionSubset ent = partitionMap.get(result);
        if (ent == null) {
          ent = new WordPartitionSubset(result);
          partitionMap.put(result, ent);
        }
        ent.add(answerIndex);
      }

      // Choose the worst case, the largest subset
      //
      WordPartitionSubset largestSubset = null;
      for (WordPartitionSubset entry : partitionMap.values()) {
        if (largestSubset == null || entry.pop() > largestSubset.pop())
          largestSubset = entry;
      }
      if (false)
        pr("examined word", queryIndex, ":", guessWord, ", largest subset size:", largestSubset.pop(),
            compareCodeString(largestSubset.compareCode()));

      if (bestGlobal == null || bestGlobal.pop() > largestSubset.pop()) {
        bestGlobal = largestSubset;
        bestQuerys.clear();
        bestPartitionMap = partitionMap;
        partitionMap = hashMap();
      }
      if (bestGlobal.pop() == largestSubset.pop())
        bestQuerys.add(queryIndex);
    }

    if (false) {
      List<WordPartitionSubset> cc = arrayList();
      cc.addAll(bestPartitionMap.values());
      cc.sort(SUBSET_COMPARATOR);

      for (WordPartitionSubset ent : cc) {
        pr(compareCodeString(ent.compareCode()), ent.pop());
      }
    }
    pr("bestQuery:", compareCodeString(bestGlobal.compareCode()));
    return bestQuerys.array();
  }

  public static List<String> sortWordsForDisplay(List<String> words) {
    List<String> common = arrayList();
    List<String> rare = arrayList();
    Dictionary comDict = WordSet.dict("mit");
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

  private static final byte[] sWork = new byte[WORD_LENGTH];
  private static final byte[] sWork2 = new byte[WORD_LENGTH];

  public static void experiment() {
    pr("running experiment");

    WordSet wordSet = WordSet.defaultSet();
    checkpoint("starting experiment");
    int[] bestGuesses = bestGuess(wordSet);
    checkpoint("done experiment"); // Takes about 9 seconds

    List<String> wordStrings = wordSet.getWordStrings(bestGuesses);
    pr("guesses:", INDENT, formatWords(wordStrings));

  }

}
