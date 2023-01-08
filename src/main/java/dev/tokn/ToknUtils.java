package dev.tokn;

import static js.base.Tools.*;

import java.util.Collection;
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
   * Build set of states reachable from this state
   */
  public static Set<State> reachableStates(State sourceState) {
    Set<State> set = hashSet();
    List<State> stack = arrayList();
    push(stack, sourceState);
    while (nonEmpty(stack)) {
      State st = pop(stack);
      set.add(st);
      for (Edge edge : st.edges()) {
        State dest = edge.destinationState();
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
  public static State reverseNFA(State startState) {

    List<RevWork> edgeList = arrayList();

    List<State> newStartStateList = arrayList();
    List<State> newFinalStateList = arrayList();

    Map<Integer, State> newStateMap = hashMap();

    Set<State> stateSet = reachableStates(startState);
    for (State s : stateSet) {

      // s.edges.each {|lbl, dest| edgeList.push([dest.id, s.id, lbl])}
      for (Edge edge : s.edges()) {
        RevWork rw = new RevWork();
        rw.source = edge.destinationState();
        rw.dest = s;
        rw.labelSet = edge.codeRanges();
        edgeList.add(rw);
      }

      State u = new State(s.id(), s.id() == startState.id(), null);
      newStateMap.put(u.id(), u);
      if (u.finalState())
        newFinalStateList.add(u);
      if (s.finalState())
        newStartStateList.add(u);

    }

    // Build a list of edges for each state, so we can modify them
    Map<Integer, List<Edge>> newStateEdgeLists = hashMap();

    for (RevWork w : edgeList) {
      State srcState = newStateMap.get(w.source.id());
      State destState = newStateMap.get(w.dest.id());

      List<Edge> edges = newStateEdgeLists.get(srcState.id());
      if (edges == null) {
        edges = arrayList();
        newStateEdgeLists.put(srcState.id(), edges);
      }
      edges.add(new Edge(w.labelSet, destState));
    }

    for (Entry<Integer, List<Edge>> edgeEntry : newStateEdgeLists.entrySet()) {
      int sourceId = edgeEntry.getKey();
      State srcState = newStateMap.get(sourceId);
      srcState = new State(sourceId, srcState.finalState(), edgeEntry.getValue());
      newStateMap.put(sourceId, srcState);
    }

    //  Create a distinguished start node that points to each of the start nodes
    int[] rang = rangeOfStateIds(stateSet);
    List<Edge> edges = arrayList();
    for (State s : newStartStateList)
      edges.add(constructEpsilonEdge(s));
    State w = new State(rang[1], false, edges);
    return w;
  }

  /**
   * Get range of state ids in a set; returns [lowest id, 1 + highest id]
   */
  public static int[] rangeOfStateIds(Collection<State> states) {
    int max_id = -1;
    int min_id = -1;
    for (State state : states) {
      if (max_id < 0) {
        max_id = state.id();
        min_id = max_id;
      } else {
        min_id = Math.min(min_id, state.id());
        max_id = Math.max(max_id, state.id());

      }
    }
    todo("Use a code range here?");
    int[] result = new int[2];
    result[0] = min_id;
    result[1] = max_id + 1;
    return result;
  }

  /**
   * Duplicate the NFA reachable from a state 
   * 
   * @param origToDupStateMap
   *          where to construct map of original state ids to new states
   */
  public static StatePair duplicateNFA(State startState, State endState, ToknContext context ) {

    pr("duplicateNFA,startState:", startState.id());

  //  checkArgument(origToDupStateMap.isEmpty());

    
    Map<Integer, State> origToDupStateMap = hashMap();

    
    
    Set<State> oldStates = reachableStates(startState);
checkState(oldStates.contains(endState),"end state not reachable");

    for (State s : oldStates) {
      State s2 = new State(context.allocateId(), s.finalState(), null);
      origToDupStateMap.put(s.id(), s2);
      pr("mapping old to new:", s.id(), s2.id());
    }

    for (State s : oldStates) {
      State s2 = origToDupStateMap.get(s.id());
      System.out.println("s.edges():"+s.edges());
      for (Edge edge : s.edges()) {

        pr("attempting to update edges for state:",s.id(),"new:",s2.id(),"edge:",edge.destinationState().id());
        State newTargetState = origToDupStateMap.get(edge.destinationState().id());

        pr("...adding modified edge:", s.id(), "=>", edge.destinationState().id(), "//", newTargetState.id());
        System.out.println("s2.edges(:"+s2.edges());
         s2.edges().add(new Edge(edge.codeRanges(), newTargetState));
      }
    }
   return statePair(origToDupStateMap.get(startState.id()), origToDupStateMap.get(endState.id()));
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

}
