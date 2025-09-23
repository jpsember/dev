package dev.prep;

import js.file.Files;

import java.io.File;
import static js.base.Tools.*;

public class DirStackEntry {

  private File mDir;
  private FilterState mFilterState;
  private DirStackEntry() {
  }

  public static DirStackEntry start(FilterState state, File directory) {
    var r = new DirStackEntry();
    r.mDir = directory;
    r.mFilterState = state;
    return r;
  }

  public DirStackEntry(FilterState state, File directory) {
    this.mDir = directory;
    this.mFilterState = state;
  }

  public DirStackEntry withState(FilterState newFilterState) {
    return new DirStackEntry(newFilterState, mDir);
  }

  public DirStackEntry withDirectory(String subdirName) {
    var newdir = new File(mDir, subdirName);
    todo("forget what's going on here");
    Files.assertDirectoryExists(newdir, "withDirectory");
    var newState = mFilterState.descendInto(subdirName);
    return new DirStackEntry(newState, newdir);
  }

  public File directory() {
    return mDir;
  }

  public FilterState filterState() {
    return mFilterState;
  }
}
