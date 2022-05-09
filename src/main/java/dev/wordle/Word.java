package dev.wordle;

import static dev.wordle.WordleUtils.*;
import static js.base.Tools.*;

public final class Word {

  public Word(String word) {
    if (word.length() != WORD_LENGTH)
      badArg("wrong length for word:", quote(word));
    mLetters = new byte[WORD_LENGTH];
    word = word.toUpperCase();
    for (int i = 0; i < WORD_LENGTH; i++)
      mLetters[i] = (byte) word.charAt(i);
  }

  public byte[] lettersArray() {
    return mLetters;
  }

  public int lettersStart() {
    return mLettersStart;
  }

  public Word(byte[] bytes, int id) {
    set(bytes, id);
  }

  private void set(byte[] sourceArray, int id) {
    mLetters = sourceArray;
    mLettersStart = id;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(WORD_LENGTH);
    for (int i = 0; i < WORD_LENGTH; i++)
      sb.append((char) mLetters[i + mLettersStart]);
    return sb.toString();
  }

  private byte[] mLetters;
  private int mLettersStart;

}
