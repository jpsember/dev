package dev.tokn;

import static js.base.Tools.*;

import java.util.List;
import java.util.Map;

import js.parsing.Edge;
import js.parsing.State;

/**
 * For modifying edges of a state machine, while still treating states as
 * immutable
 */
public class StateEdgeManager {


  public List<Edge> edgesForState(State state) {
    List<Edge> edges = mEdgeLists.get(state);
    if (edges == null) {
      edges = arrayList();
      mEdgeLists.put(state, edges);
    }
    return edges;
  }

  public void addEdge(State sourceState, int[] codeRanges, State targetState) {
    List<Edge> edges = edgesForState(sourceState);
    edges.add(new Edge(codeRanges, targetState));
  }

//  /**
//   * Construct a new version of a state, using the new edges
//   */
//  public State constructState(State newState) {
//    List<Edge> edges = edgesForState(newState);
//    return new State(newState.finalState(), edges);
//  }

  private Map<State, List<Edge>> mEdgeLists = hashMap();


}
