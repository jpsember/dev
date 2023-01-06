package dev.tokn;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import js.data.IntArray;
import js.parsing.RegExp;

import static js.base.Tools.*;

public final class DFACompiler {

  
  private static class 
 TokenEntry {
   final String name;
   final String reg_ex;
   final  int id;
    public TokenEntry(String name, String regEx, int id) {
      this.name = name; this.reg_ex = regEx;this.id = id;
    }
  }  
  
  
  private IntArray.Builder mOriginalLineNumbers;
  private List<String> mSourceLines;
  
  private static String leftTrim(String s) {
    String r = 
     (s+"|").trim();
    return r.substring(0,r.length()-1);
  }
  
  
//  Regex for token names preceding regular expressions
  private static Pattern TOKENNAME_EXPR = RegExp.pattern("[_A-Za-z][_A-Za-z0-9]*\\s*:\\s*");

  
  public void parse(String script) {
  
  
  
  
  
  
  
  
  
  
  
  int
  
    next_token_id = 0;
List<TokenEntry> token_records = arrayList();

// Maps token name to token entry
Map<String, TokenEntry> tokenNameMap = hashMap();

List<String>
        script_lines =split(script,'\n');
mOriginalLineNumbers = IntArray.newBuilder();

        // Join lines that have been ended with '\' to their following lines;
        // only do this if there's an odd number of '\' at the end


mSourceLines=arrayList();
StringBuilder accum = null; // = new StringBuilder();

       int accum_start_line = -1;
        
int originalLineNumber = 0;
for (String line : script_lines) {
  originalLineNumber++;

       int   trailing_backslash_count = 0;
          while (line.length() > trailing_backslash_count && line.charAt(-1-trailing_backslash_count) == '\\')
            trailing_backslash_count++;

          if (accum == null) {
            accum = new StringBuilder();
            accum_start_line = originalLineNumber;
          }

          if ((trailing_backslash_count &1) == 1) {
            accum.append(line.substring(0,line.length()-1));
          } else {
            accum.append(line);
            mSourceLines.add(accum.toString());
           mOriginalLineNumbers.add(accum_start_line);
           accum = null;
          }
}

        if (accum != null)
          badArg("Incomplete final line:",INDENT,script);

        // Now that we've stitched together lines where there were trailing \ characters,
        // process each line as a complete token definition

       int line_index = INIT_INDEX;
        for (String line : mSourceLines) {
          line_index++;
          int line_number = 1 +  mOriginalLineNumbers.get(line_index);

          // Strip whitespace only from the left side (which will strip all of
          // it, if the entire line is whitespace).  We want to preserve any
          // special escaped whitespace on the right side.
          line = leftTrim(line);
        

          
          // If line is empty, or starts with '#', it's a comment
          if (line.isEmpty() || line.charAt(0) == '#')
            continue;

          
          if (!RegExp.patternMatchesString(TOKENNAME_EXPR, line))
            throw badArg("Syntax error:",line_number,line);

          int
          pos = line.indexOf(":");

         String tokenName = line.substring(0,pos)
         .trim();
          
          String
          expr = line.substring(pos+1);

          RegParse    rex = new RegParse(expr, tokenNameMap, line_number);

          // Give it the next available token id, if it's not an anonymous token; else -1

         int token_id = -1;
          if (tokenName.charAt(0) != '_') {
            token_id = next_token_id;
            next_token_id++;
          }

          TokenEntry   entry = new TokenEntry (tokenName, rex, token_id);

          if  (tokenNameMap.containsKey(tokenName))
            throw badArg("Duplicate token name",line_number,line);
          
          tokenNameMap.put(entry.name,entry);

          if (entry.id < 0) continue;

          if (
          accepts_zero_characters(rex.start_state, rex.endState))
            throw badArg("Zero-length tokens accepted:",line_number,line);

          token_records.add(entry);
          

}
      State  combined = combine_token_nfas(token_records);

       NFAToDFA builder = new NFAToDFA(combined);
DFA        dfa = builder.nfa_to_dfa();

        apply_redundant_token_filter(token_records, dfa);

        
        die("not finished yet");
       // Tokn::DFA.new(token_records.map{|x| x.name}, dfa)
  
  
  
  
  
  }
  
}
