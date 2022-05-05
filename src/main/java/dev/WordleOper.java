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
import java.util.Map;

import dev.wordle.Dict;
import dev.wordle.Word;
import js.app.AppOper;
import js.app.CmdLineArgs;
import static dev.wordle.WordleUtils.*;

public class WordleOper extends AppOper {

  @Override
  public String userCommand() {
    return "wordle";
  }

  @Override
  public String getHelpDescription() {
    return "investigating Wordle strategies";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("<to be det>");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (false && args.hasNextArg()) {
      args.nextArg();
    }
    args.assertArgsDone();
  }

  private static class PartEnt {
    int population;
    int sampleWordIndex;
  }

  @Override
  public void perform() {

    if (false) 
      perf2();
    
    
    
    Dict d = Dict.standard();
    
    int[] bestGuess = bestGuess(d);
    pr(d.wordStrings(bestGuess));
  }
  
  
  private void perf2() {
    Map<Integer, PartEnt> partitionMap = hashMap();
    Word queryWord = getDictionaryWord(0);
    Word targetWord = getDictionaryWord(0);

    int ds = dictionarySize();

    PartEnt bestGlobal = null;

    for (int wordIndex = 0; wordIndex < ds; wordIndex++) {
      getDictionaryWord(queryWord, wordIndex);

      partitionMap.clear();

      for (int auxIndex = 0; auxIndex < ds; auxIndex++) {
        getDictionaryWord(targetWord, auxIndex);

        int result = compare(queryWord, targetWord);
        PartEnt ent = partitionMap.get(result);
        if (ent == null) {
          ent = new PartEnt();
          ent.sampleWordIndex = auxIndex;
          partitionMap.put(result, ent);
        }
        ent.population++;
      }

      // What is the largest population?
      PartEnt best = null;
      for (PartEnt pe : partitionMap.values()) {
        if (best == null || pe.population > best.population)
          best = pe;
      }

      if (bestGlobal == null || bestGlobal.population > best.population) {
        bestGlobal = best;

        getDictionaryWord(targetWord, bestGlobal.sampleWordIndex);

        String render = renderMatch(targetWord, compare(queryWord, targetWord));

        pr("new best word:", queryWord, "largest subset:", bestGlobal.population, "sample:", render);
      }
    }
  }


}
