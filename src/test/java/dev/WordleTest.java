/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
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
package dev;

import static js.base.Tools.*;

import java.util.List;

import org.junit.Test;

import dev.wordle.Word;
import dev.wordle.WordSet;
import dev.wordle.WordleUtils;
import js.json.JSMap;
import js.testutil.MyTestCase;

public class WordleTest extends MyTestCase {

  @Test
  public void testCompareWords() {
    WordSet wordSet = WordSet.defaultSet();

    List<Word> words = arrayList();
    for (int i = 0; i < 8; i++)
      words.add(wordSet.getWord(random().nextInt(wordSet.size())));

    JSMap m = map();
    for (Word guessWord : words) {
      JSMap m2 = map();
      m.putNumbered(guessWord.toString(), m2);
      for (Word answerWord : words) {
        int compareCode = WordleUtils.compare(answerWord, guessWord);
        m2.putNumbered(answerWord.toString(), WordleUtils.renderMatch(guessWord, compareCode));
      }
    }
    files().writePretty(generatedFile("compare.json"), m);
    log(CR, "comparison:", INDENT, m);
    assertGenerated();
  }

}
