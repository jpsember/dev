package dev.tokn;

import java.util.Map;

import static js.base.Tools.*;

import js.parsing.State;

public final class StateRenamer {

  private Map<State, State> mMap = hashMap();

  public void put(State oldState, State newState) {
    checkArgument(oldState != null);
    checkArgument(newState != null);
    State prevMapping = mMap.put(oldState, newState);
    if (prevMapping != null)
      badState("state already had a mapping!", oldState, prevMapping, "; cannot remap to", newState);
  }

  public State get(State oldState) {
    checkArgument(oldState != null);
    State newState = mMap.get(oldState);
    if (newState == null)
      badArg("no mapping found for key:", oldState);
    return newState;
  }
}
