/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package dev.wordle;

import js.base.BaseObject;

import java.util.List;

import static js.base.Tools.*;

/**
 * A set of words taken from a Dictionary
 */
public final class WordSet extends BaseObject {

  public static WordSet withWordIds(int[] wordIds) {
    WordSet d = new WordSet();
    d.mWordIds = wordIds;
    return d;
  }

  public int size() {
    return mWordIds.length;
  }

  public int wordId(int index) {
    return mWordIds[index];
  }

  public Word getWordWithId(int id) {
    return new Word(DictionaryEntry.wordBytes(), id);
  }

  public List<Word> getWords() {
    List<Word> words = arrayList();
    for (int id : mWordIds)
      words.add(new Word(DictionaryEntry.wordBytes(), id));
    return words;
  }

  public List<String> getWordStrings(int[] ids) {
    List<Word> words = getWords(ids);
    List<String> strs = arrayList();
    for (Word w : words)
      strs.add(w.toString());
    return strs;
  }

  public List<Word> getWords(int[] ids) {
    List<Word> words = arrayList();
    for (int id : ids)
      words.add(getWordWithId(id));
    return words;
  }

  public static List<String> getWordStrings(List<Word> words) {
    List<String> poss = arrayList();
    for (Word w : words)
      poss.add(w.toString());
    return poss;
  }

  public int getWordId(int wordNumber) {
    return mWordIds[wordNumber];
  }

  // Ids of words in this dictionary.  An id is its index within the master dictionary
  //
  private int[] mWordIds;

}
