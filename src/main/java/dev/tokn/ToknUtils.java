package dev.tokn;

import static js.base.Tools.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import js.parsing.Edge;
import js.parsing.State;

public final class ToknUtils {

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
        int destId = edge.destinationStateId();
        State dest = State.fetch(null, destId);
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
    int sourceId;
    int destId;
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
      State u = new State(s.id());
      newStateMap.put(u.id(), u);
      if (s.id() == startState.id()) {
        newFinalStateList.add(u);
        u.finalState(true);
      }
      if (s.finalState())
        newStartStateList.add(u);

      // s.edges.each {|lbl, dest| edgeList.push([dest.id, s.id, lbl])}
      for (Edge edge : s.edges()) {
        RevWork rw = new RevWork();
        rw.sourceId = edge.destinationStateId();
        rw.destId = s.id();
        rw.labelSet = edge.codeRanges();
        edgeList.add(rw);
      }
    }

    for (RevWork w : edgeList) {
      State srcState = newStateMap.get(w.sourceId);
      State destState = newStateMap.get(w.destId);
      srcState.edges().add(new Edge(w.labelSet, destState.id()));
    }
    //  Create a distinguished start node that points to each of the start nodes
    int[] rang = rangeOfStateIds(stateSet);
    State w = new State(rang[1]);
    for (State s : newStartStateList) {
      w.addEps(s);
    }
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
   * Duplicate the NFA reachable from this state, possibly with new ids
   * 
   * @param dupBaseId
   *          lowest state id to use for duplicates
   * @param origToDupStateMap
   *          where to construct map of original state ids to new states
   * @return next available state id
   */
  public static int duplicateNFA(State startState, int dupBaseId, Map<Integer, State> origToDupStateMap) {
    checkArgument(origToDupStateMap.isEmpty());

    Set<State> oldStates = reachableStates(startState);

    int[] res = rangeOfStateIds(oldStates);
    int oldMinId = res[0];
    int oldMaxId = res[1];

    for (State s : oldStates) {
      State s2 = new State((s.id() - oldMinId) + dupBaseId);

      s2.finalState(s.finalState());
      origToDupStateMap.put(s.id(), s2);

    }
    for (State s : oldStates) {
      State s2 = origToDupStateMap.get(s.id());
      for (Edge edge : s.edges()) {
        s2.edges().add(new Edge(edge.codeRanges(), origToDupStateMap.get(edge.destinationStateId()).id()));
      }
    }
    return (oldMaxId - oldMinId) + dupBaseId;
  }

}
