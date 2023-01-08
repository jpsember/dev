package dev.tokn;

import js.parsing.State;

public final class ToknContext {

  public void bumpIds(int minValue) {
  if (mNextId > minValue)
    mNextId += 100 - (mNextId % 100);
  else
    mNextId = minValue;
  }
  
  public int allocateId() {
    int result = mNextId;
    mNextId++;
    return result;
  }

  public State newState() {
    return newState(false);
  }

  public State newState(boolean finalFlag) {
    return new State(allocateId(), finalFlag, null);
  }

  private int mNextId = 100;
}
