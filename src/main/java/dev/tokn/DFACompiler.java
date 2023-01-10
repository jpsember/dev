package dev.tokn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import js.data.IntArray;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.Edge;
import js.parsing.RegExp;
import js.parsing.State;

import static js.base.Tools.*;

public final class DFACompiler {

  private IntArray.Builder mOriginalLineNumbers;
  private List<String> mSourceLines;

  private static String leftTrim(String s) {
    String r = (s + "|").trim();
    return r.substring(0, r.length() - 1);
  }

  //  Regex for token names preceding regular expressions
  private static Pattern TOKENNAME_EXPR = RegExp.pattern("[_A-Za-z][_A-Za-z0-9]*\\s*:\\s*.*");

  public JSMap parse(String script) {

    int next_token_id = 0;
    List<TokenEntry> token_records = arrayList();

    // Maps token name to token entry
    Map<String, TokenEntry> tokenNameMap = hashMap();

    List<String> script_lines = split(script, '\n');
    mOriginalLineNumbers = IntArray.newBuilder();

    // Join lines that have been ended with '\' to their following lines;
    // only do this if there's an odd number of '\' at the end

    mSourceLines = arrayList();
    StringBuilder accum = null;

    int accum_start_line = -1;

    int originalLineNumber = 0;

    for (String line : script_lines) {
      originalLineNumber++;

      int trailing_backslash_count = 0;
      while (true) {
        if (line.length() <= trailing_backslash_count)
          break;
        int j = line.length() - 1 - trailing_backslash_count;
        if (j < 0)
          break;
        if (line.charAt(j) != '\\')
          break;
        trailing_backslash_count++;
      }

      if (accum == null) {
        accum = new StringBuilder();
        accum_start_line = originalLineNumber;
      }

      if ((trailing_backslash_count & 1) == 1) {
        accum.append(line.substring(0, line.length() - 1));
      } else {
        accum.append(line);
        mSourceLines.add(accum.toString());
        mOriginalLineNumbers.add(accum_start_line);

        accum = null;
      }
    }

    if (accum != null)
      badArg("Incomplete final line:", INDENT, script);

    // Now that we've stitched together lines where there were trailing \ characters,
    // process each line as a complete token definition

    int line_index = INIT_INDEX;
    for (String line : mSourceLines) {
      line_index++;
      int line_number = 1 + mOriginalLineNumbers.get(line_index);

      // Strip whitespace only from the left side (which will strip all of
      // it, if the entire line is whitespace).  We want to preserve any
      // special escaped whitespace on the right side.
      line = leftTrim(line);

      // If line is empty, or starts with '#', it's a comment
      if (line.isEmpty() || line.charAt(0) == '#')
        continue;

      if (!RegExp.patternMatchesString(TOKENNAME_EXPR, line))
        throw badArg("Syntax error:", line_number, quote(line));

      int pos = line.indexOf(":");

      String tokenName = line.substring(0, pos).trim();

      String expr = line.substring(pos + 1);
      pr("============== parsing regex:", tokenName);

      RegParse rex = new RegParse(expr, tokenNameMap, line_number);

      // Give it the next available token id, if it's not an anonymous token; else -1

      int token_id = -1;
      if (tokenName.charAt(0) != '_') {
        token_id = next_token_id;
        next_token_id++;
      }

      TokenEntry entry = new TokenEntry(tokenName, rex, token_id);

      if (tokenNameMap.containsKey(tokenName))
        throw badArg("Duplicate token name", line_number, line);

      tokenNameMap.put(entry.name, entry);

      if (entry.id < 0)
        continue;

      if (accepts_zero_characters(rex.startState(), rex.endState()))
        throw badArg("Zero-length tokens accepted:", line_number, line);

      token_records.add(entry);

      pr(ToknUtils.dumpStateMachine(rex.startState(), "regex for", tokenName));
    }
    State combined = combine_token_nfas(token_records);
    pr(ToknUtils.dumpStateMachine(combined, "combined regex state machines"));

    NFAToDFA builder = new NFAToDFA();
    State dfa = builder.nfa_to_dfa(combined);
    pr(ToknUtils.dumpStateMachine(dfa, "nfa to dfa"));

    apply_redundant_token_filter(token_records, dfa);

    return constructJsonDFA(token_records, dfa);
  }

  private JSMap constructJsonDFA(List<TokenEntry> token_records, State startState) {

    pr(ToknUtils.dumpStateMachine(startState, "construct JSON DFA"));
    JSMap m = map();

    m.put("version", 3.0);

    JSList list = list();
    for (TokenEntry ent : token_records) {
      list.add(ent.name);
    }
    m.put("tokens", list);

    List<State> reachable = ToknUtils.reachableStates(startState);
    State finalState = null;

    Map<State, Integer> stateIndexMap = hashMap();
    List<State> orderedStates = arrayList();

    int index = 0;
    for (State s : reachable) {
      pr("renaming:", s, "=>", index);
      stateIndexMap.put(s, index);
      orderedStates.add(s);
      index++;
      if (!s.finalState())
        continue;
      checkState(finalState == null, "multiple final states");
      finalState = s;
    }
    checkState(stateIndexMap.get(startState) == 0, "unexpected start state index");
    checkState(finalState != null, "no final state found");
    int finalStateIndex = stateIndexMap.get(finalState);
    m.put("final", finalStateIndex);

    JSList states = list();
    for (State s : orderedStates) {
      JSList stateDesc = list();

      int edgeIndex = INIT_INDEX;
      for (Edge edge : s.edges()) {
        edgeIndex++;
        int[] cr = edge.codeRanges();
        checkArgument(cr.length >= 2);

        JSList out = list();
        for (int i = 0; i < cr.length; i+=2) {
          int a = cr[i];
          int b = cr[i+1];
          
          // Optimization:  if b==a+1, represent ...a,b,... as ...(double)a,....
          
          if (b == a+1)
            out.add((double)a);
          else {
            out.add(a);
            out.add(b);
          }
        }
        
        // Optimization: if resulting list has only one element, store that as a scalar
        if (out.size() == 1)
          stateDesc.addUnsafe(out.getUnsafe(0));
        else
          stateDesc.add(out);

        int destStateIndex = stateIndexMap.get(edge.destinationState());

        // Optimization: if last edge, and destination state is the final state, omit it
        if (edgeIndex == s.edges().size()-1 && destStateIndex == finalStateIndex)continue;
        
          stateDesc.add(destStateIndex);
      }
      states.add(stateDesc);
    }
    m.put("states", states);

    /**
     * <pre>
     * 
     * {"version":3.0,
     * "tokens":["WS","IDENTIFIER","PREFIX","EXTENDED"]
     *  "final":4,
     *  "states":[
     *       [97.0,1,   [98,123],2,     [9,11,12,14,32.0],3   ],
     *       [[98,123],2,   35.0,5,     97.0,7,   -4.0],
     *       [[97,123],2,   35.0,5   ],
     *       [[9,11,12,14,32.0],3,     -2.0],
     *       [],
     *       [[65,91,95.0,97,123],6],
     *       [[48,58,65,91,95.0,97,123],6,-3.0],
     *       [[97.0,99,123],2,35.0,5,98.0,8],
     *       [[97,123],2,35.0,5,-5.0]
     *   ]}
     * </pre>
     */
    return m;
  }

  /** Determine if regex accepts zero characters */
  private static boolean accepts_zero_characters(State start_state, State end_state) {
    Set<State> marked_states = hashSet();
    List<State> state_stack = arrayList();
    push(state_stack, start_state);
    while (nonEmpty(state_stack)) {
      State state = pop(state_stack);
      if (marked_states.contains(state))
        continue;
      marked_states.add(state);
      if (state == end_state)
        return true;

      if (todo("refactor this code now that id is no longer there"))
        return false;
      //      for (Edge edge : state.edges()) {
      //        if (CodeSet.contains(edge.codeRanges(), edge.destinationState().id())) {
      //          push(state_stack, edge.destinationState());
      //        }
      //      }
    }
    return false;
  }

  /**
   * Combine the individual NFAs constructed for the token definitions into one
   * large NFA, each augmented with an edge labelled with the appropriate token
   * identifier to let the tokenizer see which token led to the final state.
   * 
   * @param context
   */
  private State combine_token_nfas(List<TokenEntry> token_records) {

    // Create a new distinguished start state
    //
    State start_state = new State();
    for (TokenEntry tk : token_records) {
      RegParse regParse = tk.reg_ex;

      StatePair newStates = ToknUtils.duplicateNFA(regParse.startState(), regParse.endState());

      State dupStart = newStates.start;

      // Transition from the expression's end state (not a final state)
      // to a new final state, with the transitioning edge
      // labelled with the token id (actually, a transformed token id to distinguish
      // it from character codes)
      State dupEnd = newStates.end;
      State dupfinal_state = new State(true);

      //  List<Edge> edges = arrayList();
      //  edges.add(ToknUtils.constructEpsilonEdge(dupfinal_state));

      CodeSet cs = CodeSet.withValue(State.tokenIdToEdgeLabel(tk.id));
      dupEnd.edges().add(new Edge(cs.elements(), dupfinal_state));

      // Add an e-transition from the start state to this expression's start

      ToknUtils.addEps(start_state, dupStart);
    }
    return start_state;
  }

  //  private static void addEdge(State source, CodeSet codeSet, State target) {
  //    Edge edge = new Edge(codeSet.elements(), target.id());
  //  }

  /**
   * Determine if any tokens are redundant, and report an error if so
   */
  private void apply_redundant_token_filter(List<TokenEntry> token_records, State start_state) {

    Set<Integer> recognized_token_id_set = treeSet();

    for (State state : ToknUtils.reachableStates(start_state)) {
      for (Edge edge : state.edges()) {
        if (!edge.destinationState().finalState())
          continue;
        int token_id = State.edgeLabelToTokenId(edge.codeRanges()[0]);
        recognized_token_id_set.add(token_id);
      }
    }

    List<String> unrecognized = arrayList();

    int z = INIT_INDEX;
    for (TokenEntry rec : token_records) {
      z++;
      checkState(z == rec.id, "index:", z, "rec id:", rec.id);
      if (recognized_token_id_set.contains(rec.id))
        continue;
      unrecognized.add(rec.name);
    }

    pr(ToknUtils.dumpStateMachine(start_state, "apply_redundant_token_filter"));

    if (nonEmpty(unrecognized))
      badState("Redundant token(s) found:", unrecognized);
    //
    //    start_state.reachable_states.each do |state|
    //      state.edges.each do |label, dest|
    //        next unless dest.final_state
    //        token_id = ToknInternal::edge_label_to_token_id(label.elements[0])
    //        recognized_token_id_set.add(token_id)
    //      end
    //    end
    //
    //    unrecognized = []
    //
    //    token_records.each do |rec|
    //      next if recognized_token_id_set.include? rec.id
    //      unrecognized << rec.name
    //    end
    //
    //    return if unrecognized.empty?
    //
    //    raise ParseException, "Redundant token(s) found: #{unrecognized.join(", ")}"
    //  end
  }

}
