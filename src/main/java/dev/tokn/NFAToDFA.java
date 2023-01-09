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
  public NFAToDFA(State start_state) {
    todo("pass start state to nfa_to_dfa function, avoid lifetime issues with this class");
    mStartState = start_state;
    mWithFilter = true;
  }

  public void withFilter(boolean f) {
    mWithFilter = false;
  }

  private State mStartState;
  private boolean mWithFilter;

  //    attr_reader :start_state
  //    attr_accessor :with_filter
  //    attr_accessor :generate_pdf
  //
  //    def initialize(start_state)
  //      @start_state = start_state
  //      @with_filter = true
  //      @generate_pdf = false
  //    end
  //
  /**
   * Convert an NFA to a DFA; return the new start state
   */
  public State nfa_to_dfa() {

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
    mStartState = ToknUtils.reverseNFA(mStartState);
    nfa_to_dfa_aux();
    pr("reversing #2");
    mStartState = ToknUtils.reverseNFA(mStartState);
    nfa_to_dfa_aux();
    normalizeStates(mStartState);
  }

  /**
   * Convert NFA to DFA
   */
  private void nfa_to_dfa_aux() {

    //      @nextId = 0
    //
    // Build a map of nfa state ids => nfa states
    mNFAStateMap = hashMap();

    //      nfas = start_state.reachable_states
    //      nfas.each {|s| @nfaStateMap[s.id] = s}
    //

    // Initialize a map of nfa state lists, keyed by dfa states
    //
    // TODO: rename later, as this is not a state 'id' list
    sorted_nfa_state_id_lists = hashMap();

    //@sorted_nfa_state_id_lists = []

    Set<State> iset = hashSet();
    iset.add(mStartState);
    eps_closure(iset);

    State.bumpDebugIds();

    //
    //      new_start_state,_ = create_dfa_state_if_necessary(states_to_sorted_ids(iset))
    State new_start_state = create_dfa_state_if_necessary(iset);

    List<State> unmarked = arrayList();
    unmarked.add(new_start_state);

    while (nonEmpty(unmarked)) {
      State dfaState = pop(unmarked);

      Collection<State> nfaIds = sorted_nfa_state_id_lists.get(dfaState);
      checkState(nfaIds != null);

      // Map of CodeSet => set of NFA states
      Map<CodeSet, Set<State>> moveMap = hashMap();

      //        # map of CodeSet => set of NFA states
      //        moveMap = {}
      //
      for (State nfaState : nfaIds) {
        for (Edge nfaEdge : nfaState.edges()) {
          CodeSet codeSet = CodeSet.with(nfaEdge.codeRanges());
          if (codeSet.contains(State.EPSILON))
            continue;

          Set<State> nfaStates = moveMap.get(codeSet);
          if (nfaStates == null) {
            nfaStates = hashSet();
            moveMap.put(codeSet, nfaStates);
          }
          nfaStates.add(nfaEdge.destinationState());
        }
        //        nfaIds.each do |nfaId|
        //          nfaState = @nfaStateMap[nfaId]
        //          nfaState.edges.each do |lbl,dest|
        //            if lbl.elements[0] == EPSILON
        //              next
        //            end
        //
        //            nfaStates = moveMap[lbl]
        //            if nfaStates.nil?
        //              nfaStates = Set.new
        //              moveMap[lbl] = nfaStates
        //            end
        //            nfaStates.add(dest)
        //          end
        //        end
        //
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
    //        moveMap.each_pair do |charRange,nfaStates|
    //          # May be better to test if already in set before calc closure; or simply has closure
    //          eps_closure(nfaStates)
    //          dfaDestState, isNew = create_dfa_state_if_necessary(states_to_sorted_ids(nfaStates))
    //          if isNew
    //            unmarked.push(dfaDestState)
    //          end
    //          dfaState.addEdge(charRange, dfaDestState)
    //        end
    //
    //      end
    //
    //      @start_state = new_start_state
    //    end
  }

  // Modify edges so each is labelled with a disjoint subset
  //  of characters.  See the notes at the start of this class,
  //  as well as RangePartition.rb.
  //  
  private void partition_edges() {
    RangePartition par = new RangePartition();
    List<State> stateSet = ToknUtils.reachableStates(mStartState);
    for (State s : stateSet) {
      for (Edge edge : s.edges()) {
        // TODO: unnecessary wrapping int[] within CodeSet
        par.addSet(CodeSet.with(edge.codeRanges()));
      }
    }
    par.prepare();
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
    }
    //
    //      par.prepare
    //
    //      stateSet.each do |s|
    //        newEdges = []
    //        s.edges.each do |lbl, dest|
    //          newLbls = par.apply(lbl)
    //          newLbls.each {|x| newEdges.push([x, dest]) }
    //        end
    //        s.clearEdges()
    //
    //        newEdges.each do |lbl,dest|
    //          s.addEdge(lbl,dest)
    //        end
    //      end
    //
    //    end
    //
  }

  private static String keyForInts(List<Integer> ints) {
    StringBuilder sb = new StringBuilder();
    for (Integer k : ints) {
      sb.append(' ');
      sb.append(k);
    }
    return sb.toString().trim();
  }

  /**
   * Determine if a DFA state exists for a set of NFA states, and add one if
   * not. Sets mDFAStateCreatedFlag true iff a new state was created
   *
   * @return DFA state
   */
  private State create_dfa_state_if_necessary(Collection<State> stateSet) {
    mDFAStateCreatedFlag = false;
    List<Integer> idList = arrayList();
    for (State s : stateSet)
      idList.add(s.debugId());
    idList.sort(null);
    String key = keyForInts(idList);

    State newState = mNFAStateMap.get(key);
    if (newState == null) {
      mDFAStateCreatedFlag = true;
      newState = new State();
      // Determine if any of the NFA states were final states
      for (State nfaState : stateSet)
        if (nfaState.finalState()) {
          newState.setFinal(true);
          break;
        }
      mNFAStateMap.put(key, newState);
      sorted_nfa_state_id_lists.put(newState, stateSet);
      pr("created DFA state", newState, "for set of NFA ids:", key);
    }
    return newState;
  }

  //    # for the set
  //    #
  //    # @param sorted_nfa_state_id_list a sorted array of NFA state ids
  //    # @return a pair [DFA State,
  //    #                 created flag (boolean): true if this did not already exist]
  //    #
  //    def create_dfa_state_if_necessary(sorted_nfa_state_id_list)
  //      newState = @nfaStateMap[sorted_nfa_state_id_list]
  //      isNewState = newState.nil?
  //      if isNewState
  //        newState = State.new(@nextId)
  //
  //        # Determine if any of the NFA states were final states
  //        newState.final_state = sorted_nfa_state_id_list.any?{|id| @nfaStateMap[id].final_state}
  //
  //        @nextId += 1
  //        @nfaStateMap[sorted_nfa_state_id_list] = newState
  //        @sorted_nfa_state_id_lists.push(sorted_nfa_state_id_list)
  //      end
  //      return [newState,isNewState]
  //    end
  //
  //    def states_to_sorted_ids(s)
  //      s.to_a.map {|x| x.id}.sort
  //    end
  //

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
  public static void normalizeStates(State startState) {
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
  public static void normalize(State state) {
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

  private Map<String, State> mNFAStateMap;
  private Map<State, Collection<State>> sorted_nfa_state_id_lists;
  private boolean mDFAStateCreatedFlag;
}
