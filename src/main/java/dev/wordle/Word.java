package dev.wordle;

import static dev.wordle.WordleUtils.*;
import static js.base.Tools.*;

public final class Word {

  public static Word buildEmpty() {
    return new Word();
  }

  private Word() {
  }

  public Word(String word) {
    set(word);
  }

  public byte[] letters() {
    return mLetters;
  }

  public Word(byte[] bytes, int wordNumber) {
    set(bytes, wordNumber);
  }

  public void set(byte[] sourceArray, int wordNumber) {
    int j = wordNumber * WORD_LENGTH;
    for (int i = 0; i < WORD_LENGTH; i++)
      mLetters[i] = sourceArray[j + i];
  }

  public void set(String word) {
    if (word.length() != WORD_LENGTH)
      badArg("wrong length for word:", quote(word));
    word = word.toUpperCase();
    for (int i = 0; i < WORD_LENGTH; i++)
      mLetters[i] = (byte) word.charAt(i);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(WORD_LENGTH);
    for (int i = 0; i < WORD_LENGTH; i++)
      sb.append((char) mLetters[i]);
    return sb.toString();
  }

  private byte[] mLetters = new byte[WORD_LENGTH];

  public void readLetters(byte[] target) {
    for (int i = 0; i < WORD_LENGTH; i++)
      target[i] = mLetters[i];
  }

}
