package dev.tokn;

public final class ToknContext {

  public int allocateId() {
    int result = mNextId;
    mNextId++;
    return result;
  }

  private int mNextId;
}
