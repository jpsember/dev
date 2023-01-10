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
package dev;

import static js.base.Tools.*;

import org.junit.Test;

import dev.tokn.DFACompiler;
import js.file.FileException;
import js.file.Files;
import js.json.JSMap;
import js.parsing.DFA;
import js.parsing.Scanner;
import js.testutil.MyTestCase;

public class CompileTest extends MyTestCase {

  @Test
  public void simple() {
    proc("abbaaa");
  }

  @Test
  public void complex() { 
    proc("// comment\n1234\n  'hello'  ");
  }

  @Test
  public void complex1() {
    proc();
  }

  @Test
  public void identifier() {
    proc("  alpha123 123 123alpha123");
  }

  @Test
  public void alpha() {
    proc(" if if  ifif  ");
  }

  private String testName() {
    parseName();
    return mTestName;
  }

  private int testVersion() {
    parseName();
    return mVersion;
  }

  private void parseName() {
    // If unit test name is xxxx123, extract the suffix 123 as the version for the input
    if (mTestName == null) {
      String nm = name();
      String testName = nm;
      int j = nm.length();
      while (true) {
        char c = nm.charAt(j - 1);
        if (!(c >= '0' && c <= '9'))
          break;
        j--;
      }
      if (j < nm.length()) {
        mVersion = Integer.parseInt(nm.substring(j));
        testName = nm.substring(0, j);
      }
      mTestName = testName;
    }
  }

  private String mTestName;
  private int mVersion = -1;

  private void proc() {
    proc(null);
  }

  private void proc(String sampleText) {

    String resourceName = testName() + ".rxp";
    mScript = Files.readString(this.getClass(), resourceName);

    DFACompiler c = new DFACompiler();
    mDFAJson = c.parse(mScript);

    files().writeString(generatedFile("dfa.json"), mDFAJson.prettyPrint());

    if (sampleText == null) {
      // If there's a sample text file, read it
      String filename = testName() + ".txt";
      if (testVersion() >= 0)
        filename = testName() + testVersion() + ".txt";
      try {
        sampleText = Files.readString(this.getClass(), filename);
      } catch (FileException e) {
      }
      if (sampleText == null && testVersion() >= 0)
        badArg("missing sample text file:", filename);
    }

    if (sampleText != null) {
      StringBuilder sb = new StringBuilder();

      // Don't skip any tokens
      Scanner s = new Scanner(dfa(), sampleText, -1);
      s.setVerbose(verbose());
      while (s.hasNext()) {
        sb.append(s.read());
        sb.append('\n');
      }
      String result = sb.toString();
      log("Parsed tokens:", INDENT, result);
      files().writeString(generatedFile("tokens.txt"), result);
    }

    assertGenerated();
  }

  private DFA dfa() {
    loadTools();
    if (mDFA == null)
      mDFA = new DFA(mDFAJson);
    return mDFA;
  }

  private String mScript;
  private JSMap mDFAJson;
  private DFA mDFA;
}
