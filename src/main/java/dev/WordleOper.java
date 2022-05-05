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
import java.util.NoSuchElementException;
import java.util.Scanner;

import dev.wordle.WordSet;
import dev.wordle.Guess;
import dev.wordle.Word;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.data.IntArray;

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

  private void help() {
    Object t = TAB(20);
    pr("Commands:", INDENT, //
        "a*ppl'e", t, "make a guess", CR, //
        "a", t, "advice", CR, //
        "d [ big | mit ]", CR, //
        "h", t, "help", CR, //
        "n", t, "new game", CR, //
        "q", t, "quit");
  }

  @Override
  public void perform() {
    newGame();

    pr("Type 'h' for help");
    Scanner input = new Scanner(System.in);

    boolean quit = false;
    while (!quit) {
      System.out.print("> ");
      String s;
      try {
        s = input.nextLine().trim().toLowerCase();
      } catch (NoSuchElementException e) {
        s = "q";
      }
      switch (s) {
      case "q":
        quit = true;
        break;
      case "n":
        newGame();
        break;
      case "a":
        advice();
        break;
      case "h":
      case "help":
        help();
        break;
      default:
        parseCommand(s);
        break;
      }
    }
  }

  private void newGame() {
    g = new GameVars();
  }

  private void advice() {
    if (dict().size() > 100) {
      pr("(" + dict().size() + " solutions)");
    } else {
      List<String> poss = arrayList();
      for (Word w : dict().getWords())
        poss.add(w.toString());
      pr("Solutions:", INDENT, formatWords(poss));
    }
    pr("Guesses:", INDENT, formatWords(bestGuesses()));
  }

  private static String formatWords(List<String> words) {
    StringBuilder sb = new StringBuilder();
    int i = INIT_INDEX;
    for (String w : words) {
      i++;
      sb.append(w);
      sb.append((i % 8) == 7 ? '\n' : ' ');
    }
    return sb.toString().toLowerCase();
  }

  private WordSet dict() {
    if (g.dict == null) {
      g.dict = WordSet.defaultSet();
    }
    return g.dict;
  }

  private List<String> bestGuesses() {
    if (g.bestGuesses == null) {
      if (g.turnNumber == 0)
        g.bestGuesses = WordSet.defaultDictionary().initialGuesses();
      else
        g.bestGuesses = dict().getWordStrings(bestGuess(dict()));
    }
    return g.bestGuesses;
  }

  private class GameVars {
    WordSet dict;
    List<String> bestGuesses;
    int turnNumber;
  }

  private GameVars g = new GameVars();

  private void parseCommand(String cmd) {
    if (cmd.startsWith("d ")) {
      String name = cmd.substring(2);
      pr("...selecting dictionary:", name);
      WordSet.selectDictionary(name);
      newGame();
      return;
    }

    Guess g = Guess.parse(cmd);
    if (g != null) {
      makeGuess(g);
      advice();
    } else {
      pr("*** Don't understand:", cmd);
    }
  }

  private void makeGuess(Guess guess) {
    WordSet dict = dict();
    int dictSize = dict.size();

    IntArray.Builder ib = IntArray.newBuilder();
    Word queryWord = new Word(guess.word());
    Word targetWord = Word.buildEmpty();

    for (int targetIndex = 0; targetIndex < dictSize; targetIndex++) {
      dict.getWord(targetWord, targetIndex);
      int result = compare(targetWord, queryWord);
      if (result != guess.compareResult())
        continue;
      ib.add(dict.wordId(targetIndex));
    }
    dict = WordSet.withWordIds(ib.array());
    g.dict = dict;
    g.bestGuesses = null;
    g.turnNumber++;

    pr("...turn number", g.turnNumber);
  }

}
