package dev.tokn;

import js.parsing.Edge;
import js.parsing.State;
import static js.base.Tools.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Converts NFAs (nondeterministic, finite state automata) to minimal DFAs.
 * 
 * Performs the subset construction algorithm described in (among other places)
 * http://en.wikipedia.org/wiki/Powerset_construction
 * 
 * Also implements an innovative algorithm to partition a set of edge labels
 * into a set that has the property that no two elements have overlapping
 * regions. This allows us to perform the subset construction (and closure
 * operations) efficiently while supporting large possible character sets (e.g.,
 * unicode, which ranges from 0..0x10ffff. See RangePartition.rb for more
 * details.
 *
 */
public class NFAToDFA {

  public NFAToDFA withFilter(boolean f) {
    mWithFilter = f;
    return this;
  }

  /**
   * Convert an NFA to a DFA; return the new start state
   */
  public State nfa_to_dfa(State start_state) {
    checkState(mStartState == null, "already used");
    mStartState = start_state;

    partition_edges();
    minimize();
    if (mWithFilter && !alert("skipping any filtering, as I suspect it doesn't do much")) {
      Filter filter = new Filter(mStartState);
      filter.apply();
      if (filter.modified()) {
        //  Re-minimize the dfa, since it's been modified by the filter
        minimize();
      }
    }
    return mStartState;
  }

  /**
   * Construct minimized dfa from nfa
   */
  private void minimize() {

    // Reverse this NFA, convert to DFA, then reverse it, and convert it again.  
    // Apparently this  produces a minimal DFA.
    //

    pr("reversing #1");
    //pr(ToknUtils.dumpStateMachine(mStartState, "before reverse #1"));
    mStartState = ToknUtils.reverseNFA(mStartState);
    pr(ToknUtils.dumpStateMachine(mStartState, "after reverse #1"));

    nfa_to_dfa_aux();

    pr("reversing #2");
    mStartState = ToknUtils.reverseNFA(mStartState);
    pr(ToknUtils.dumpStateMachine(mStartState, "after reverse #2"));
    nfa_to_dfa_aux();
    normalizeStates(mStartState);
  }

  private static CodeSet constructKeyForStateCollection(Collection<State> states) {
    CodeSet keySet = new CodeSet();
    for (State s : states)
      keySet.add(s.debugId());
    return keySet;
  }

  /**
   * Convert NFA to DFA
   */
  private void nfa_to_dfa_aux() {

    mNFAStateSetToDFAStateMap = hashMap();

    if (true) {
      for (State state : ToknUtils.reachableStates(mStartState)) {
        List<State> list = arrayList();
        list.add(state);
        CodeSet keySet = constructKeyForStateCollection(list);
        mNFAStateSetToDFAStateMap.put(keySet, state);
        pr("storing in NFA state map, key:", keySet, "state:", state);
      }
    }

    // Initialize a map of nfa state lists, keyed by dfa states
    //
    // TODO: rename later, as this is not a state 'id' list
    sorted_nfa_state_id_lists = hashMap();

    Set<State> iset = hashSet();
    iset.add(mStartState);
    eps_closure(iset);

    State.bumpDebugIds();

    State new_start_state = create_dfa_state_if_necessary(iset);

    List<State> unmarked = arrayList();
    unmarked.add(new_start_state);

    pr("nfa_to_dfa loop start");
    while (nonEmpty(unmarked)) {
      State dfaState = pop(unmarked);
      pr("unmarked state:", dfaState);

      Collection<State> nfaIds = sorted_nfa_state_id_lists.get(dfaState);
      pr("nfaIds:", nfaIds);

      if (nfaIds == null)
        badState("dfaState had no entry in sorted_nfa_state_id_lists:", dfaState);

      // Map of CodeSet => set of NFA states
      Map<CodeSet, Set<State>> moveMap = hashMap();

      for (State nfaState : nfaIds) {

        pr("examining nfa state:", nfaState.toString(true));

        for (Edge nfaEdge : nfaState.edges()) {
          pr("edge:", nfaEdge);
          CodeSet codeSet = CodeSet.with(nfaEdge.codeRanges());

          if (codeSet.contains(State.EPSILON)) {
            continue;
          }

          pr("looking in moveMap for key:", codeSet);
          Set<State> nfaStates = moveMap.get(codeSet);
          if (nfaStates == null) {
            nfaStates = hashSet();
            moveMap.put(codeSet, nfaStates);
          }
          nfaStates.add(nfaEdge.destinationState());
        }
      }

      for (Entry<CodeSet, Set<State>> moveMapEntry : moveMap.entrySet()) {
        CodeSet codeSet = moveMapEntry.getKey();
        Set<State> nfaStates = moveMapEntry.getValue();
        eps_closure(nfaStates);

        State dfaDestState = create_dfa_state_if_necessary(nfaStates);
        if (mDFAStateCreatedFlag)
          unmarked.add(dfaDestState);
        dfaState.edges().add(new Edge(codeSet.elements(), dfaDestState));
      }
    }
    mStartState = new_start_state;
  }

  /**
   * Modify edges so each is labelled with a disjoint subset of characters.
   */
  private void partition_edges() {
    final boolean db = false && alert("db is on");
    if (db)
      pr("partition_edges");
    RangePartition par = new RangePartition();

    List<State> stateSet = ToknUtils.reachableStates(mStartState);
    if (db)
      pr("reachable states:", INDENT, stateSet);

    for (State s : stateSet) {
      for (Edge edge : s.edges()) {
        // TODO: unnecessary wrapping int[] within CodeSet
        par.addSet(CodeSet.with(edge.codeRanges()));
      }
    }

    for (State s : stateSet) {
      List<Edge> newEdges = arrayList();
      for (Edge edge : s.edges()) {
        List<CodeSet> newLbls = par.apply(CodeSet.with(edge.codeRanges()));
        for (CodeSet x : newLbls) {
          push(newEdges, new Edge(x.elements(), edge.destinationState()));
        }

      }
      s.edges().clear();
      s.edges().addAll(newEdges);
      if (db)
        pr("partition edges, state now:", INDENT, s.toString(true));
    }
  }

  /**
   * Determine if a DFA state exists for a set of NFA states, and add one if
   * not. Sets mDFAStateCreatedFlag true iff a new state was created
   *
   * @return DFA state; also, mDFAStateCreatedFlag will be set iff a new state
   *         was created
   */
  private State create_dfa_state_if_necessary(Collection<State> stateSet) {
    mDFAStateCreatedFlag = false;

    CodeSet keySet = constructKeyForStateCollection(stateSet);

    pr("create_dfa_state_if_necessary for:", keySet);

    State newState = mNFAStateSetToDFAStateMap.get(keySet);
    if (newState == null) {
      mDFAStateCreatedFlag = true;
      newState = new State();
      // Determine if any of the NFA states were final states
      for (State nfaState : stateSet)
        if (nfaState.finalState()) {
          newState.setFinal(true);
          break;
        }
      mNFAStateSetToDFAStateMap.put(keySet, newState);
      sorted_nfa_state_id_lists.put(newState, stateSet);
      pr("created DFA state", newState, "for set of NFA ids:", keySet);
    }
    return newState;
  }

  private boolean mDFAStateCreatedFlag;

  /**
   * Calculate the epsilon closure of a set of NFA states
   */
  private void eps_closure(Set<State> stateSet) {
    List<State> stk = arrayList();
    stk.addAll(stateSet);
    while (nonEmpty(stk)) {
      State s = pop(stk);
      for (Edge edge : s.edges()) {
        if (CodeSet.contains(edge.codeRanges(), State.EPSILON)) {
          if (stateSet.add(edge.destinationState()))
            push(stk, edge.destinationState());
        }
      }
    }
  }

  /**
   * Normalize a state machine.
   *
   * <pre>
  * For each state:
  *  [] merge edges that go to a common state
  *  [] delete edges that have empty labels
  *  [] sort edges by destination state ids
   * 
   * </pre>
   */
  private static void normalizeStates(State startState) {
    List<State> reachable = ToknUtils.reachableStates(startState);
    for (State s : reachable) {
      normalize(s);
    }
  }

  /**
   * Normalize a state
   * 
   * <pre>
    *  [] merge edges that go to a common state
    *  [] sort edges by destination state debug ids
    *  [] delete edges that have empty labels
   * </pre>
   */
  private static void normalize(State state) {
    List<Edge> edgeList = arrayList();
    edgeList.addAll(state.edges());
    edgeList
        .sort((e1, e2) -> Integer.compare(e1.destinationState().debugId(), e2.destinationState().debugId()));

    List<Edge> new_edges = arrayList();
    CodeSet prev_label = null;
    State prev_dest = null;

    for (Edge edge : edgeList) {
      int[] label = edge.codeRanges();
      State dest = edge.destinationState();

      // If this edge goes to the same state as the previous one (they are in sorted order already), merge with that one...
      if (prev_dest == dest)
        prev_label.addSet(label);
      else {
        if (prev_dest != null) {
          new_edges.add(new Edge(prev_label.elements(), prev_dest));
        }
        // Must start a fresh copy!  Don't want to modify the original label.
        prev_label = CodeSet.with(label);
        prev_dest = edge.destinationState();
      }
    }

    if (prev_dest != null)
      new_edges.add(new Edge(prev_label.elements(), prev_dest));

    state.setEdges(new_edges);
  }

  private boolean mWithFilter = true;

  private State mStartState;

  // A map of NFA id sets to NFA states.  
  // Each NFA id set is represented by a CodeSet, since they support equals+hashcode methods
  //
  private Map<CodeSet, State> mNFAStateSetToDFAStateMap;
  private Map<State, Collection<State>> sorted_nfa_state_id_lists;

}
