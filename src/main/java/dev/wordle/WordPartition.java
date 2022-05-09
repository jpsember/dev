package dev.wordle;

import static dev.wordle.WordleUtils.*;

@Deprecated
public class WordPartition {

  public WordPartition() {
    mSubsets = new WordPartitionSubset[SUBSET_TOTAL];
    for (int i = 0; i < SUBSET_TOTAL; i++) {
      mSubsets[i] = new WordPartitionSubset(i);
    }
  }


  private static final int SUBSET_TOTAL = (int) Math.pow(3, WORD_LENGTH);
  private final WordPartitionSubset[] mSubsets;

}
