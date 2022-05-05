package dev.wordle;

import static js.base.Tools.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import js.data.ByteArray;
import js.data.IntArray;
import js.file.Files;

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

  static {
    loadTools();
  }

  private static final void clearWork(byte[] a) {
    for (int i = 0; i < WORD_LENGTH; i++)
      a[i] = 0;
  }

  public static int dictionarySize() {
    if (sDictSize == 0)
      wordList();
    return sDictSize;
  }

  public static Word getDictionaryWord(int index) {
    return new Word(wordList(), index);
  }

  public static void getDictionaryWord(Word mainWord, int wordIndex) {
    mainWord.set(wordList(), wordIndex);
  }

  public static byte[] wordList() {
    if (sWordList == null) {

      if (false) {
        String s = Files.readString(WordleUtils.class, "mit_list.txt").toUpperCase().trim() + "\n";
        List<String> words = split(s, '\n');
        List<String> filt = arrayList();
        for (String st : words)
          if (st.length() == WORD_LENGTH)
            filt.add(st);
        File target = Files.getDesktopFile("mit_words.txt");
        Files.S.writeString(target, String.join("\n", filt));
        halt("wrote:", target);
      }

      String listName = "wordle_list.txt";
      if (true)
        listName = "mit_5.txt";
      String s = Files.readString(WordleUtils.class, listName).toUpperCase().trim() + "\n";

      int origSize = s.length() / (WORD_LENGTH + 1);

      int subsetSize = 12947;
      {
        int subList = (WORD_LENGTH + 1) * subsetSize;
        if (subList < s.length()) {
          if (alert("using smaller dictionary,", subsetSize, "<", origSize))
            s = s.substring(0, subList);
        }
      }
      byte[] sourceBytes;
      try {
        sourceBytes = s.getBytes("UTF-8");
      } catch (Throwable e) {
        throw asRuntimeException(e);
      }
      ByteArray.Builder target = ByteArray.newBuilder();
      int cursor = 0;
      while (cursor < sourceBytes.length) {
        for (int i = 0; i < WORD_LENGTH; i++)
          target.add(sourceBytes[cursor + i]);
        cursor += WORD_LENGTH + 1;
      }
      sWordList = target.array();
      sDictSize = sWordList.length / WORD_LENGTH;
    }
    return sWordList;
  }

  private static class PartEnt {
    //    int population;
    //    int sampleWordIndex;
    final IntArray.Builder set = IntArray.newBuilder();

    int pop() {
      return set.size();
    }

    void add(int wordIndex) {
      set.add(wordIndex);
    }
  }

  /**
   * Given a set of possible answers, determine the best guesses
   */
  public static int[] bestGuess(Dict dict) {

    Map<Integer, PartEnt> partitionMap = hashMap();
    Word queryWord = Word.buildEmpty();
    Word targetWord = Word.buildEmpty();

    int ds = dict.size();

    PartEnt bestGlobal = null;

    for (int wordIndex = 0; wordIndex < ds; wordIndex++) {
      dict.getWord(queryWord, wordIndex);

      partitionMap.clear();

      for (int auxIndex = 0; auxIndex < ds; auxIndex++) {
        dict.getWord(targetWord, auxIndex);

        int result = compare(queryWord, targetWord);
        PartEnt ent = partitionMap.get(result);
        if (ent == null) {
          ent = new PartEnt();
          partitionMap.put(result, ent);
        }
        ent.add(auxIndex);
      }

      // What is the largest population?
      PartEnt best = null;
      for (PartEnt pe : partitionMap.values()) {
        if (best == null || pe.pop() > best.pop())
          best = pe;
      }

      if (bestGlobal == null || bestGlobal.pop() > best.pop()) {
        bestGlobal = best;

        dict.getWord(targetWord, bestGlobal.set.get(0));

        String render = renderMatch(targetWord, compare(queryWord, targetWord));

        pr("new best word:", queryWord, "largest subset:", bestGlobal.pop(), "sample:", render);
      }
    }

    return bestGlobal.set.array();
  }

  private static final byte[] sWork = new byte[WORD_LENGTH];
  private static final byte[] sWork2 = new byte[WORD_LENGTH];
  private static byte[] sWordList;
  private static int sDictSize;
}
