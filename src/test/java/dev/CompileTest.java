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
import static org.junit.Assert.*;

import org.junit.Test;

import dev.tokn.CodeSet;
import dev.tokn.DFACompiler;
import js.data.IntArray;
import js.file.Files;
import js.parsing.DFA;
import js.testutil.MyTestCase;
import static dev.tokn.TokenConst.*;

public class CompileTest extends MyTestCase {

  @Test
  public void simple() {
    proc();
  halt("name:",name());
  }
  private void proc() {
  String testName = name();
  String resourceName = testName+".txt";
  String script = 
  Files.readString(this.getClass(),resourceName);comp(script);
  }
  
  
  @Test
  public void x() {
    String scr = 
        
        "# abc\\\n" //
        +"# def\\\n" +//
        "# Sample token definitions\n"
        + "\n" //
        + "# Whitespace includes a comment, which starts with '//' and\n" //
        + "# extends to the end of the line; or, c-style comments /* ... */\n" //
        + "#\n" //
        + "# 0\n" //
        + "WS:   ( [\\f\\r\\s\\t\\n]+ ) \\\n" //
        + "    | ( // [^\\n]* \\n? ) \\\n" //
        + "    | ( /\\*  ((\\** [^*/] ) | [^*])*  \\*+ / )\n" //
        + "\n" //
        + "# c-style comment can be described as:\n" //
        + "#\n" //
        + "# '/*'\n" //
        + "# followed by any number of either\n" //
        + "#      any number of * followed by something other than * or /\n" //
        + "#   or something other than *\n" //
        + "# followed by one or more *\n" //
        + "# followed by /\n" //
        + "#\n" //
        + "\n" //
        + "# An anonymous token, for convenience; a non-empty sequence of digits\n" //
        + "#\n" //
        + "_DIG: \\d+\n" //
        + "\n" //
        + "# Double has higher priority than int, since we don't want the prefix of the double\n" //
        + "# to be intpreted as an int\n" //
        + "\n" //
        + "# 1\n" //
        + "INT: \\-?$DIG\n" //
        + "\n" //
        + "# 2\n" //
        + "#\n" //
        + "DBL: \\-?([0] | ([1-9]\\d*)) . \\d+\n" //
        + "\n" //
        + "# 3\n" //
        + "LBL: '([^'\\n]|\\\\')*'\n" //
        + "\n" //
        + "# 4\n" //
        + "ID:  [_a-zA-Z]\\w*\n" //
        + "\n" //
        + "# 5\n" //
        + "ASSIGN: =\n" //
        + "\n" //
        + "# 6\n" //
        + "EQUIV: ==\n" //
        + "\n" //
        + "# 7\n" //
        + "IF: if\n" //
        + "\n" //
        + "# 8\n" //
        + "DO: do\n" //
        + "\n" //
        + "# 9\n" //
        + "BROP: \\{\n" //
        + "\n" //
        + "# 10\n" //
        + "BRCL: \\}\n" //
        ;
    
    
    comp(scr);
  }
private void comp(String script) {
  DFACompiler c = new DFACompiler();
  DFA result = 
  c.parse(script);
  
}


}
