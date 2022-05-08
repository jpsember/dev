package dev.wordle;

import js.data.IntArray;

/**
 * A subset of words, each keyed by a CompareCode, a collection of which form
 * a partition of a WordSet
 */
class WordPartitionSubset {

  private final IntArray.Builder mSet = IntArray.newBuilder();
  private final int mCompareCode;

  public int compareCode() {
    return mCompareCode;
  }

  int pop() {
    return mSet.size();
  }

  void add(int wordIndex) {
    mSet.add(wordIndex);
  }

  WordPartitionSubset(int compareCode) {
    mCompareCode = compareCode;
  }
}