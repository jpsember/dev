package dev.wordle;

import js.data.IntArray;

/**
 * A subset of words, each keyed by a CompareCode, a collection of which form a
 * partition of a WordSet
 */
@Deprecated
class WordPartitionSubset {

  public int compareCode() {
    return mCompareCode;
  }

  public int pop() {
    return mSet.size();
  }

  public void add(int wordIndex) {
    mSet.add(wordIndex);
  }

  public WordPartitionSubset(int compareCode) {
    mCompareCode = compareCode;
  }

  /**
   * Empty the current set
   */
  public void clear() {
    mSet.clear();
  }

  public static void clear(WordPartitionSubset[] subsets) {
    for (WordPartitionSubset s : subsets)
      s.clear();
  }

  private final IntArray.Builder mSet = IntArray.newBuilder();
  private final int mCompareCode;

}