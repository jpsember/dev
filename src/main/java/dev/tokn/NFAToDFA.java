package dev.tokn;

import js.parsing.Edge;
import js.parsing.State;
import static js.base.Tools.*;

import java.util.List;
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
    if (mWithFilter) {
      Filter filter = new Filter(mStartState);
      filter.apply();
      if (filter.modified()) {
        //  Re-minimize the dfa, since it's been modified by the filter
        minimize();
      }
    }
    return mStartState;
  }

  // Construct minimized dfa from nfa
  private void minimize() {
    // Reverse this NFA, convert to DFA, then reverse it, and convert it again.  
    // Apparently this  produces a minimal DFA.
    //

    mStartState = ToknUtils.reverseNFA(mStartState);
    nfa_to_dfa_aux();
    mStartState = ToknUtils.reverseNFA(mStartState);
    nfa_to_dfa_aux();
    normalizeStates(mStartState);
  }

  //    end
  //
  // Perform the build algorithm
  private void nfa_to_dfa_aux() {
    //      @nextId = 0
    //
    //      # Build a map of nfa state ids => nfa states
    //      @nfaStateMap = {}
    //      nfas = start_state.reachable_states
    //      nfas.each {|s| @nfaStateMap[s.id] = s}
    //
    //      # Initialize an array of nfa state lists, indexed by dfa state id
    //      @sorted_nfa_state_id_lists = []
    //
    //      # Map of existing DFA states; key is array of NFA state ids
    //      @dfaStateMap = {}
    //
    //      iset = Set.new
    //      iset.add(start_state)
    //      eps_closure(iset)
    //
    //      new_start_state,_ = create_dfa_state_if_necessary(states_to_sorted_ids(iset))
    //
    //      unmarked = [new_start_state]
    //
    //      until unmarked.empty?
    //        dfaState  = unmarked.pop
    //
    //        nfaIds = @sorted_nfa_state_id_lists[dfaState.id]
    //
    //        # map of CodeSet => set of NFA states
    //        moveMap = {}
    //
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
    notFinished();
  }

  // Modify edges so each is labelled with a disjoint subset
  //  of characters.  See the notes at the start of this class,
  //  as well as RangePartition.rb.
  //  
  private void partition_edges() {
    //
    //      par = RangePartition.new
    //
    //      stateSet = @start_state.reachable_states
    //
    //      stateSet.each do |s|
    //        s.edges.each {|lbl,dest| par.addSet(lbl) }
    //      end
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
    notFinished();
  }
  //    # Adds a DFA state for a set of NFA states, if one doesn't already exist
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
  //    # Calculate the epsilon closure of a set of NFA states
  //    #
  //    def eps_closure(stateSet)
  //      stk = stateSet.to_a
  //      while !stk.empty?
  //        s = stk.pop
  //        s.edges.each do |lbl,dest|
  //          if lbl.contains? EPSILON
  //            if stateSet.add?(dest)
  //              stk.push(dest)
  //            end
  //          end
  //        end
  //      end
  //    end
  //
  //  end # class NFAToDFA
  //
  //end  # module ToknInternal

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
    Set<State> reachable = ToknUtils.reachableStates(startState);
    for (State s : reachable) {
      normalize(s);
    }
  }

  /**
   * Normalize a state
   * 
   * <pre>
    *  [] merge edges that go to a common state
    *  [] delete edges that have empty labels
    *  [] sort edges by destination state ids
   * </pre>
   */
  public static State normalize(State state) {
    List<Edge> edgeList = arrayList();
    edgeList.addAll(state.edges());
    edgeList.sort((e1, e2) -> Integer.compare(e1.destinationStateId(), e2.destinationStateId()));

    List<Edge> new_edges = arrayList();
    CodeSet prev_label = null;
    int prev_dest = -1;

    for (Edge edge : edgeList) {
      int[] label = edge.codeRanges();
      int dest = edge.destinationStateId();

      // If this edge goes to the same state as a previous one, merge with that one...
      todo("probably can't assume previous is meaningful, as they may be in a random order");
      if (prev_dest == dest)
        prev_label.addSet(label);
      else {
        if (prev_dest >= 0) {
          new_edges.add(new Edge(prev_label.elements(), prev_dest));
        }
        // Must start a fresh copy!  Don't want to modify the original label.
        prev_label = CodeSet.with(label);
        prev_dest = edge.destinationStateId();
      }
    }
    if (prev_dest >= 0)
      new_edges.add(new Edge(prev_label.elements(), prev_dest));
    State newState = new State(state.id(), state.finalState(), new_edges);
    halt("we should return a new, normalized state");
    return newState;
  }

}
