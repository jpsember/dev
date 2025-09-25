package dev.strip;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static js.base.Tools.*;

public class FilterState extends BaseObject {


  public FilterState(File containerDir, Collection<File> deleteFiles) {
    mDirectoryAbs = Files.assertAbsolute(containerDir);
    for (var f : deleteFiles)
      mDeleteFilesAbs.add(Files.assertAbsolute(f));
  }

  /**
   * Construct a new FilterState by descending into a subdirectory of the current state's directory
   */
  public FilterState descendInto(File absSubdir) {
    Files.assertAbsolute(absSubdir);
    var rel = Files.relativeToContainingDirectory(absSubdir, this.directory());
    log("descendInto:", rel);
    var fs = new FilterState(absSubdir, this.deleteFilesAbs());
    log("...returning:", INDENT, fs);
    return fs;
  }

  public Set<File> deleteFilesAbs() {
    return mDeleteFilesAbs;
  }

  public File directory() {
    return mDirectoryAbs;
  }

  private final File mDirectoryAbs;

  // This map should be considered immutable.  If changes are made, construct a new copy
  private final Set<File> mDeleteFilesAbs = hashSet();

  @Override
  public JSMap toJson() {
    var m = map();
    m.put("dir", directory().toString());
    List<String> lst = arrayList();
    if (!mDeleteFilesAbs.isEmpty()) {
      for (var x : mDeleteFilesAbs)
        lst.add(x.toString());
      lst.sort(null);
      m.put("x_del", JSList.with(lst));
    }
    return m;
  }
}
