package dev.wordle;

import js.base.BaseObject;

import static dev.wordle.WordleUtils.*;

import java.util.List;
import static js.base.Tools.*;

public final class Dict extends BaseObject {

  public static Dict standard() {
    if (sDefaultDict == null) {
      int k = dictionarySize();
      int[] w = new int[k];
      for (int i = 0; i < k; i++)
        w[i] = i;
      sDefaultDict = withWords(w);
    }
    return sDefaultDict;
  }

  public static Dict withWords(int[] wordInds) {
    Dict d = new Dict();
    d.mWordInds = wordInds;
    return d;
  }

  public int size() {
    return mWordInds.length;
  }

  public void getWord(Word target, int index) {
    byte[] wl = wordList();
    target.set(wl, mWordInds[index]);
  }

  public Word getWord(int index) {
    return new Word(wordList(), mWordInds[index]);
  }

  public List<String> wordStrings(int[] wordIndices) {
    Word work = Word.buildEmpty();
    List<String> result = arrayList();
    for (int x : wordIndices) {
      getWord(work, x);
      result.add(work.toString());
    }
    return result;
  }

  private int[] mWordInds;
  private static Dict sDefaultDict;

}
