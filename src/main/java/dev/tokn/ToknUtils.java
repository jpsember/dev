package dev.tokn;

import static js.base.Tools.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import js.base.BasePrinter;
import js.parsing.Edge;
import js.parsing.State;

public final class ToknUtils {

  @Deprecated // Create an 'add edge' utility method
  public static BigEdge newEdge(State sourceState, int[] codeSet, State destinationState) {
    return new BigEdge(sourceState, codeSet, destinationState);
  }

  public static void addEdge(State sourceState, int[] codeSet, State destinationState) {
    sourceState.edges().add(new BigEdge(sourceState, codeSet, destinationState));
  }

  public static void addEdge(State sourceState, CodeSet codeSet, State destinationState) {
    addEdge(sourceState, codeSet.elements(), destinationState);
  }

  /**
   * Build set of states reachable from this state
   */
  public static List<State> reachableStates(State sourceState) {

    final boolean db = false;

    if (db)
      pr("reachableStates from:", sourceState);

    Set<State> knownStatesSet = hashSet();
    List<State> scanStack = arrayList();
    List<State> output = arrayList();
    push(scanStack, sourceState);
    knownStatesSet.add(sourceState);

    while (nonEmpty(scanStack)) {

      State st = pop(scanStack);
      output.add(st);
      if (db)
        pr(st.toString(true));

      for (Edge edge : st.edges()) {
        State dest = edge.destinationState();
        if (knownStatesSet.add(dest))
          push(scanStack, dest);
      }
    }

    if (db)
      pr("reachable set:", State.toString(knownStatesSet));
    return output;
  }

  /**
   * Construct the reverse of an NFA
   * 
   * @param startState
   *          start state for NFA
   * @return start state of reversed NFA
   */
  public static State reverseNFA(State startState) {

    final boolean db = true;
    if (db) {
      pr(dumpStateMachine(startState, "reverseNFA:"));
    }

    State.bumpDebugIds();

    // Create new start state first, so it has the lowest id
    State newStartState = new State();

    List<State> newStartStateList = arrayList();
    List<State> newFinalStateList = arrayList();

    StateRenamer newStateMap = new StateRenamer();

    List<State> stateSet = reachableStates(startState);
    if (db)
      pr("reachable states from", startState, INDENT, State.toString(stateSet));

    for (State s : stateSet) {
      State newState = newStateMap.put(s, new State(s == startState));
      if (newState.finalState())
        newFinalStateList.add(newState);
      if (s.finalState())
        newStartStateList.add(newState);
    }

    StateEdgeManager em = new StateEdgeManager();

    for (State oldState : stateSet) {
      State newState = newStateMap.get(oldState);
      for (Edge oldEdge : oldState.edges()) {
        State oldDest = oldEdge.destinationState();
        State newDest = newStateMap.get(oldDest);
        // We want a reversed edge
        em.addEdge(newDest, oldEdge.codeRanges(), newState);
      }
    }

    for (State oldS : stateSet) {
      State newState = newStateMap.get(oldS);
      newState.setEdges(em.edgesForState(newState));
    }

    //  Make start node point to each of the reversed start nodes

    for (State s : newStartStateList)
      newStartState.edges().add(constructEpsilonEdge(newStartState, s));
    if (db) {
      pr("new start state:", newStartState);
      pr(dumpStateMachine(newStartState, "Reversed:"));
    }

    return newStartState;
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
  public static StatePair duplicateNFA(State startState, State endState) {

    Map<State, State> origToDupStateMap = hashMap();

    List<State> oldStates = reachableStates(startState);
    checkState(oldStates.contains(endState), "end state not reachable");

    for (State s : oldStates) {
      State s2 = new State(s.finalState(), null);
      origToDupStateMap.put(s, s2);
    }

    for (State s : oldStates) {
      State s2 = origToDupStateMap.get(s);
      for (Edge edge : s.edges()) {
        State newTargetState = origToDupStateMap.get(edge.destinationState());
        s2.edges().add(newEdge(s2, edge.codeRanges(), newTargetState));
      }
    }
    return statePair(origToDupStateMap.get(startState), origToDupStateMap.get(endState));
  }

  private static int[] EPSILON_RANGE = { State.EPSILON, 1 + State.EPSILON };

  /**
   * Add an epsilon transition to a state
   */
  public static void addEps(State source, State target) {
    source.edges().add(newEdge(source, EPSILON_RANGE, target));
  }

  public static Edge constructEpsilonEdge(State source, State target) {
    return newEdge(source, EPSILON_RANGE, target);
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

    StringBuilder sb = new StringBuilder("{");
    int i = 0;
    while (i < elements.length) {
      if (i > 0)
        sb.append(' ');

      int lower = elements[i];
      int upper = elements[i + 1];
      sb.append(element_to_s(lower));
      if (upper != 1 + lower) {
        sb.append("..");
        sb.append(element_to_s(upper - 1));
      }
      i += 2;
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get a debug description of a value within a CodeSet
   */
  private static String element_to_s(int charCode) {

    final String forbidden = "\'\"\\[]{}()";

    // Unless it corresponds to a non-confusing printable ASCII value,
    // just print its decimal equivalent
    if (charCode == State.EPSILON)
      return "(e)";
    if (charCode > ' ' && charCode < 0x7f && forbidden.indexOf(charCode) < 0)
      return "'" + Character.toString((char) charCode) + "'";
    if (charCode == State.CODEMAX - 1)
      return "MAX";
    return Integer.toString(charCode);
  }

  public static boolean equal(CodeSet a, CodeSet b) {
    return Arrays.equals(a.elements(), b.elements());
  }

  public static String dumpStateMachine(State initialState, Object... title) {
    StringBuilder sb = new StringBuilder();
    sb.append("====== State Machine");
    if (title.length != 0) {
      sb.append(" : ");
      sb.append(BasePrinter.toString(title));
    }
    sb.append('\n');

    List<State> reachableStates = reachableStates(initialState);

    // Sort them by their debug ids
    reachableStates.sort(null);

    // But make sure the start state is first
    sb.append(toString(initialState, true));
    for (State s : reachableStates) {
      if (s == initialState)
        continue;
      sb.append(toString(s, true));
    }
    sb.append("=======================================================\n");
    return sb.toString();
  }

  public static String toString(State state, boolean includeEdges) {
    StringBuilder sb = new StringBuilder();
    sb.append(state.debugId());
    sb.append(state.finalState() ? '*' : ' ');
    if (includeEdges) {
      sb.append("=>\n");
      for (Edge e : state.edges()) {
        sb.append("       ");
        sb.append(e.destinationState().debugId());
        sb.append(' ');
        sb.append(dumpCodeRange(e.codeRanges()));
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  private static String toString(Edge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(dumpCodeRange(edge.codeRanges()));
    sb.append(" => ");
    sb.append(edge.destinationState().debugId());
    return sb.toString();
  }

  static {
    BasePrinter.registerClassHandler(Edge.class, (x, p) -> p.append(toString((Edge) x)));
  }

}
