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
import java.util.Random;
import java.util.Scanner;

import dev.wordle.WordSet;
import dev.gen.wordle.Dictionary;
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
        "n [<answer>]", t, "new game", CR, //
        "q", t, "quit");
  }

  @Override
  public void perform() {
    if (false) {
      experiment();
      return;
    }
    pr("Type 'h' for help");
    newGame();
    //advice();

    Scanner input = new Scanner(System.in);

    boolean quit = false;
    while (!quit) {
      System.out.print("> ");
      String s = "";
      try {
        try {
          s = input.nextLine().trim().toLowerCase();
        } catch (NoSuchElementException e) {
          s = "q";
        }

        mArgs = split(s, ' ');
        mCursor = 0;

        String a = readArg();
        switch (a) {
        case "q":
          quit = true;
          break;
        case "n":
          newGame();
          if (hasNextArg()) {
            g.answer = new Word(readArg());
          }
          if (false)
          advice();
          break;
        case "a":
          advice();
          break;
        case "d": {
          String name = readArg();
          pr("...selecting dictionary:", name);
          WordSet.selectDictionary(name);
          newGame();
        }
          break;
        case "h":
        case "help":
          help();
          break;
        default:
          if (g.gameOver) {
            pr("Game is over!");
            break;
          }
          parseCommand(a);
          break;
        }
        checkArgument(!hasNextArg());
      } catch (IllegalArgumentException e) {
        pr("Can't understand:", quote(s));
      }
    }
  }

  private List<String> mArgs;
  private int mCursor;

  private String readArg() {
    checkArgument(hasNextArg());
    return mArgs.get(mCursor++);
  }

  private boolean hasNextArg() {
    return mCursor < mArgs.size();
  }

  private void newGame() {
    g = new GameVars();

    // Choose an answer from the small dictionary

    Dictionary d = WordSet.dict("mit");
    byte[] by = d.wordBytes();
    int nw = by.length / WORD_LENGTH;
    g.answer = new Word(by, WORD_LENGTH * mRand.nextInt(nw));
    pr(VERT_SP);
  }

  private Random mRand = new Random();

  private void advice() {
    if (g.gameOver) {
      pr("...type n to play again");
      return;
    }

    if (dict().size() > 200) {
      pr("Answers:", dict().size());
    } else {
      dict();
      List<String> poss = WordSet.getWordStrings(dict().getWords());
      poss = sortWordsForDisplay(poss);
      pr("Answers:", INDENT, formatWords(poss));
    }
    pr("Suggestions:", INDENT, formatWords(bestGuesses()));
  }

  private WordSet dict() {
    if (g.dict == null) {
      g.dict = WordSet.defaultSet();
    }
    return g.dict;
  }

  private List<String> bestGuesses() {
   // if (g.bestGuesses == null) {
      if (turnNumber() == 0 && WITH_FIRST_GUESS_OPTIMIZATION)
        g.bestGuesses = WordSet.defaultDictionary().initialGuesses();
      else
        g.bestGuesses = dict().getWordStrings(bestGuess(dict()));
      g.bestGuesses = sortWordsForDisplay(g.bestGuesses);
 //   }
   return g.bestGuesses;
  }

  private class GameVars {
    WordSet dict;
    Word answer;
    List<String> bestGuesses;
    List<Guess> guesses = arrayList();
    boolean gameOver;
  }

  private GameVars g = new GameVars();

  private boolean answerIsKnown() {
    return g.answer != null;
  }

  private void parseCommand(String cmd) {
    Guess gu = Guess.parse(cmd);
    checkArgument(gu != null);
    if (answerIsKnown()) {
      checkArgument(gu.compareCode() == 0);
      int result = compare(g.answer, gu.word());
      gu = Guess.with(gu.word(), result);
    }
    makeGuess(gu);
    // advice();
  }

  private void makeGuess(Guess guess) {
    WordSet dict = dict();
    int dictSize = dict.size();

    byte[] dictWords = WordSet.defaultDictionary().wordBytes();
    IntArray.Builder ib = IntArray.newBuilder();
    Word guessWord = guess.word();

    for (int answerIndex = 0; answerIndex < dictSize; answerIndex++) {
      int answerWordOffset = dict.getWordId(answerIndex);

      int result = compareOpt(dictWords, answerWordOffset, guessWord.lettersArray(),
          guessWord.lettersStart());
      if (result != guess.compareCode())
        continue;
      ib.add(answerWordOffset);
    }
    dict = WordSet.withWordIds(ib.array());
    g.dict = dict;
    g.bestGuesses = null;
    g.guesses.add(guess);
    if (guess.compareCode() == COMPARE_CODE_FINISHED)
      g.gameOver = true;

    pr(VERT_SP);
    int guessNum = INIT_INDEX;
    for (Guess g : g.guesses) {
      guessNum++;
      pr(guessNum + 1, ":", renderMatch(g.word(), g.compareCode()));
    }
    pr(VERT_SP);
  }

  private int turnNumber() {
    return g.guesses.size();
  }

}
