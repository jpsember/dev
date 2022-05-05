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
import java.util.Random;
import java.util.Scanner;

import dev.wordle.Dict;
import dev.wordle.Word;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.json.JSList;

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

  @Override
  public void perform() {
    if (false) {
      perf2();
      return;
    }

    newGame();
    Scanner input = new Scanner(System.in);

    boolean quit = false;
    while (!quit) {
      System.out.print("> ");
      String s = input.nextLine().trim().toLowerCase();
      switch (s) {
      case "q":
        quit = true;
        break;
      case "n":
        newGame();
        break;
      default:
        parseCommand(s);
        break;
      }
    }
  }

  private void newGame() {
  }

  private void parseCommand(String cmd) {
    pr("*** Don't understand:", cmd);
  }

  private void perf2() {

    Dict d = Dict.standard();

    int guessNumber = 0;
    while (d.size() > 1) {
      guessNumber++;
      checkState(guessNumber < 20);

      int[] bestGuessesList = bestGuess(d);
      checkState(bestGuessesList.length > 0);

      if (false && alert("stopping"))
        break;
      int guessIndex = rand().nextInt(bestGuessesList.length);
      int guess = bestGuessesList[guessIndex];

      Word w = d.getWord(guess);
      pr("Guess #" + guessNumber + ":", w);

      pr("resulting possibilities:", INDENT, JSList.with(d.wordStrings(bestGuessesList)));
      d = Dict.withWords(bestGuessesList);
    }
  }

  private Random rand() {
    if (mRandom == null)
      mRandom = new Random(1965);
    return mRandom;
  }

  private Random mRandom;

}
