package dev.wordle;

import static js.base.Tools.*;

import java.util.Map;

import dev.gen.wordle.Dictionary;
import js.data.IntArray;

public final class WordleUtils {

  public static final int WORD_LENGTH = 5;

  private static final int CODE_SKIP = '.';

  public static final int MATCH_NONE = 0;
  public static final int MATCH_PARTIAL = 1;
  public static final int MATCH_FULL = 2;

  public static Word word(String text) {
    return new Word(text);
  }

  public static int compare(Word target, Word query) {
    byte[] matchCodes = sWork;
    clearWork(matchCodes);

    byte[] targetCopy = sWork2;
    target.readLetters(targetCopy);
    byte[] queryBytes = query.letters();

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (queryBytes[i] == targetCopy[i]) {
        matchCodes[i] = MATCH_FULL;
        targetCopy[i] = CODE_SKIP;
      }
    }

    for (int i = 0; i < WORD_LENGTH; i++) {
      if (matchCodes[i] != MATCH_NONE)
        continue;
      int q = queryBytes[i];
      for (int j = 0; j < WORD_LENGTH; j++) {
        if (targetCopy[j] == CODE_SKIP)
          continue;
        if (targetCopy[j] == q) {
          matchCodes[i] = MATCH_PARTIAL;
          targetCopy[j] = CODE_SKIP;
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

  private static final void clearWork(byte[] a) {
    for (int i = 0; i < WORD_LENGTH; i++)
      a[i] = 0;
  }

  public static Word getDictionaryWord(int index) {
    return new Word(wordList(), index);
  }

  public static void getDictionaryWord(Word mainWord, int wordIndex) {
    mainWord.set(wordList(), wordIndex);
  }

  public static Dictionary dictionary() {
    if (sDictionary == null) {
      try {
        todo("calling this from Dict and then calling Dict again");
        Dictionary dict = Dict.readDictionary("mit");
        Dictionary.Builder db = dict.toBuilder();
        byte[] b = new byte[dict.words().size() * WORD_LENGTH];
        int c = 0;
        for (String s : dict.words()) {
          byte[] sourceBytes = s.getBytes("UTF-8");
          System.arraycopy(sourceBytes, 0, b, c, WORD_LENGTH);
          c += WORD_LENGTH;
        }
        db.wordBytes(b);
        sDictionary = db.build();
      } catch (Throwable e) {
        throw asRuntimeException(e);
      }
    }
    return sDictionary;
  }

  public static byte[] wordList() {
    return dictionary().wordBytes();
  }

  private static class PartitionEntry {
    final IntArray.Builder set = IntArray.newBuilder();
    final int compareResult;

    int pop() {
      return set.size();
    }

    void add(int wordIndex) {
      set.add(wordIndex);
    }

    PartitionEntry(int compareResult) {
      this.compareResult = compareResult;
    }
  }

  /**
   * Given a set of possible answers, determine the best guesses
   */
  public static int[] bestGuess(Dict dict) {

    Map<Integer, PartitionEntry> partitionMap = hashMap();
    Word queryWord = Word.buildEmpty();
    Word targetWord = Word.buildEmpty();
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

    PartitionEntry bestGlobal = null;

    IntArray.Builder bestQuerys = IntArray.newBuilder();

    for (int queryIndex = 0; queryIndex < dictSize; queryIndex++) {
      dict.getWord(queryWord, queryIndex);

      partitionMap.clear();

      for (int targetIndex = 0; targetIndex < dictSize; targetIndex++) {
        dict.getWord(targetWord, targetIndex);

        int result = compare(targetWord, queryWord);
        PartitionEntry ent = partitionMap.get(result);
        if (ent == null) {
          ent = new PartitionEntry(result);
          partitionMap.put(result, ent);
        }
        ent.add(targetIndex);
      }

      // Choose the worst case, the largest subset
      //
      PartitionEntry largestSubset = null;
      for (PartitionEntry entry : partitionMap.values()) {
        if (largestSubset == null || entry.pop() > largestSubset.pop())
          largestSubset = entry;
      }
      if (false)
        pr("examined word", queryIndex, ":", queryWord, ", largest subset size:", largestSubset.pop(),
            largestSubset.compareResult);

      if (bestGlobal == null || bestGlobal.pop() > largestSubset.pop()) {
        bestGlobal = largestSubset;
        bestQuerys.clear();
      }
      if (bestGlobal.pop() == largestSubset.pop())
        bestQuerys.add(queryIndex);
    }

    return bestQuerys.array();
  }

  private static final byte[] sWork = new byte[WORD_LENGTH];
  private static final byte[] sWork2 = new byte[WORD_LENGTH];

  private static Dictionary sDictionary;
}
