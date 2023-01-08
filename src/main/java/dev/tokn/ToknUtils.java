package dev.tokn;

import static js.base.Tools.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import js.parsing.Edge;
import js.parsing.State;

public final class ToknUtils {

  /**
   * edge label for epsilon transitions
   */
  public static final int EPSILON = -1;

  /**
   * One plus the maximum code represented
   */
  public static final int CODEMAX = 0x110000;

  /**
   * Minimum code possible (e.g., indicating a token id)
   */
  public static final int CODEMIN = -10000;

  /**
   * Build set of states reachable from this state
   */
  public static Set<State> reachableStates(State sourceState) {

    final boolean db = true;

    if (db)
      pr("reachableStates from:", sourceState);

    Set<State> set = hashSet();
    List<State> stack = arrayList();
    push(stack, sourceState);
    while (nonEmpty(stack)) {
      State st = pop(stack);
      set.add(st);

      pr(st.toString(true));

      for (Edge edge : st.edges()) {
        State dest = edge.destinationState();
        pr(" => ", dest, "(", dumpCodeRange(edge.codeRanges()), ")");

        if (set.add(dest))
          push(stack, dest);
      }
    }
    return set;
  }

  /**
   * Bookkeeping class for reversing NFA
   */
  private static class RevWork {
    State source;
    State dest;
    int[] labelSet;
  }

  /**
   * Construct the reverse of an NFA
   * 
   * @param startState
   *          start state for NFA
   * @return start state of reversed NFA
   */
  public static State reverseNFA( State startState) {

    pr("reversing NFA");

    List<RevWork> edgeList = arrayList();

    List<State> newStartStateList = arrayList();
    List<State> newFinalStateList = arrayList();

    StateRenamer newStateMap = new StateRenamer();
    //    Map<State, State> newStateMap = hashMap();
    StateRenamer newerStateMap = new StateRenamer();

    Set<State> stateSet = reachableStates(startState);
    for (State s : stateSet) {

      pr("processing state:", s);
      // s.edges.each {|lbl, dest| edgeList.push([dest.id, s.id, lbl])}
      for (Edge edge : s.edges()) {
        RevWork rw = new RevWork();
        rw.source = edge.destinationState();
        rw.dest = s;
        rw.labelSet = edge.codeRanges();
        edgeList.add(rw);
      }

      State u = new State(s == startState, null);
      pr("converted state to:", u);
      newerStateMap.put(s, u);

      //  oldToNewStateIdMap.put(s.id(), u.id());
      newStateMap.put(s, u);
      if (u.finalState())
        newFinalStateList.add(u);
      if (s.finalState())
        newStartStateList.add(u);

    }

    // Build a list of edges for each state, so we can modify them
    Map<State, List<Edge>> newStateEdgeLists = hashMap();

    for (RevWork w : edgeList) {
      State srcState = newStateMap.get(w.source );
      State destState = newStateMap.get(w.dest );

      List<Edge> edges = newStateEdgeLists.get(srcState);
      if (edges == null) {
        edges = arrayList();
     //   newStateEdgeLists.put(srcState.id(), edges);
      }
      edges.add(new Edge(w.labelSet, destState));
    }

    for (Entry<State, List<Edge>> edgeEntry : newStateEdgeLists.entrySet()) {
      State oldSrcState = edgeEntry.getKey();
      State srcState = newerStateMap.get(oldSrcState);

      halt("getting crazy... lost in remapping these ids");
      srcState = new State(  srcState.finalState(), edgeEntry.getValue());
     // newerStateMap.put(sourceId, srcState);
    }

    //  Create a distinguished start node that points to each of the start nodes
    List<Edge> edges = arrayList();
    for (State s : newStartStateList)
      edges.add(constructEpsilonEdge(s));
    State w = new State(false, edges);
    return w;
  }

  //  /**
  //   * Get range of state ids in a set; returns [lowest id, 1 + highest id]
  //   */
  //  @Deprecated
  //  public static int[] rangeOfStateIds(Collection<State> states) {
  //    int max_id = -1;
  //    int min_id = -1;
  //    for (State state : states) {
  //      if (max_id < 0) {
  //        max_id = state.id();
  //        min_id = max_id;
  //      } else {
  //        min_id = Math.min(min_id, state.id());
  //        max_id = Math.max(max_id, state.id());
  //
  //      }
  //    }
  //    todo("Use a code range here?");
  //    int[] result = new int[2];
  //    result[0] = min_id;
  //    result[1] = max_id + 1;
  //    return result;
  //  }

  /**
   * Duplicate the NFA reachable from a state
   * 
   * @param origToDupStateMap
   *          where to construct map of original state ids to new states
   */
  public static StatePair duplicateNFA(State startState, State endState ) {

    Map<State, State> origToDupStateMap = hashMap();

    Set<State> oldStates = reachableStates(startState);
    checkState(oldStates.contains(endState), "end state not reachable");

    for (State s : oldStates) {
      State s2 = new State(s.finalState(), null);
      origToDupStateMap.put(s, s2);
    }

    for (State s : oldStates) {
      State s2 = origToDupStateMap.get(s);
      for (Edge edge : s.edges()) {
        State newTargetState = origToDupStateMap.get(edge.destinationState());
        s2.edges().add(new Edge(edge.codeRanges(), newTargetState));
      }
    }
    return statePair(origToDupStateMap.get(startState), origToDupStateMap.get(endState));
  }

  private static int[] EPSILON_RANGE = { EPSILON, 1 + EPSILON };

  /**
   * Add an epsilon transition to a state
   */
  public static void addEps(State source, State target) {
    source.edges().add(new Edge(EPSILON_RANGE, target));
  }

  public static Edge constructEpsilonEdge(State target) {
    return new Edge(EPSILON_RANGE, target);
  }

  public static StatePair statePair(State start, State end) {
    checkNotNull(start);
    checkNotNull(end);
    StatePair sp = new StatePair();
    sp.start = start;
    sp.end = end;
    return sp;
  }


  public static String dumpCodeRange(int[] elements) {
    checkArgument((elements.length & 1) == 0);

    StringBuilder s = new StringBuilder();
    int i = 0;
    while (i < elements.length) {
      if (s.length() > 0)
        s.append(' ');

      int lower = elements[i];
      int upper = elements[i + 1];
      s.append(element_to_s(lower));
      if (upper != 1 + lower) {
        s.append("..");
        s.append(element_to_s(upper - 1));
      }
      i += 2;
    }
    return s.toString();
  }

  /**
   * Get a debug description of a value within a CodeSet
   */
  private static String element_to_s(int charCode) {

    final String forbidden = "\'\"\\[]{}()";

    // Unless it corresponds to a non-confusing printable ASCII value,
    // just print its decimal equivalent
    if (charCode == EPSILON)
      return "(e)";
    if (charCode > ' ' && charCode < 0x7f && forbidden.indexOf(charCode) < 0)
      return "'" + Character.toString((char) charCode) + "'";
    if (charCode == CODEMAX - 1)
      return "MAX";
    return Integer.toString(charCode);
  }

  public static boolean equal(CodeSet a, CodeSet b) {
    return Arrays.equals(a.elements(), b.elements());
  }

}
