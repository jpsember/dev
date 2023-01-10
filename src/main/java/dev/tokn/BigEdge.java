package dev.tokn;

import js.parsing.Edge;
import js.parsing.State;

public class BigEdge extends Edge {

  public BigEdge(State sourceState, int[] codeRanges, State destState) {
    super(codeRanges, destState);
    mSourceState = sourceState;
  }

  @Override
  public State sourceState() {
    return mSourceState;
  }

  private final State mSourceState;

}
