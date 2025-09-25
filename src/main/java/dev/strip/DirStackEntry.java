package dev.strip;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSMap;

import java.io.File;

@Deprecated
public class DirStackEntry extends BaseObject {


  private DirStackEntry() {
  }

  public static DirStackEntry start(FilterState state, File directory) {
    var r = new DirStackEntry();
    r.mDir = directory;
    r.mFilterState = state;
    return r;
  }

  private DirStackEntry(FilterState state, File directory) {
    this.mDir = directory;
    this.mFilterState = state;
  }

  public DirStackEntry withState(FilterState newFilterState) {
    return new DirStackEntry(newFilterState, mDir);
  }

  public DirStackEntry withSubDirectory(File subdir) {
    // Verify that subdirectory is strictly within the current directory
    Files.assertAbsolute(subdir);
    File rel = Files.relativeToContainingDirectory(subdir, mDir);
//    var newdir = subdir; //new File(mDir, subdirName);
    Files.assertDirectoryExists(subdir, "withDirectory");
    var newState = mFilterState.descendInto(rel);
    return new DirStackEntry(newState, subdir);
  }

  public File directory() {
    return mDir;
  }

  public FilterState filterState() {
    return mFilterState;
  }

  @Override
  public JSMap toJson() {
    var m = super.toJson();
    m.put("dir", mDir.toString());
    m.put("filter_state", mFilterState.toJson());
    return m;
  }

  private File mDir;
  private FilterState mFilterState;

}
