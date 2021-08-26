package dev;

import static js.base.Tools.*;

import java.io.File;

import js.file.Files;

public final class Utils {

  /**
   * Find a file within a project directory structure, by ascending to a parent
   * directory until found (or we run out of parents)
   * 
   * @param startParentDirectoryOrNull
   *          directory to start search within, or null for current directory
   * @param filename
   *          name of file (or directory) to look for
   * @return found file, or Files.DEFAULT
   */
  public static File optFileWithinParents(File startParentDirectoryOrNull, String filename) {
    loadTools();
    File dir;
    if (startParentDirectoryOrNull == null)
      dir = Files.currentDirectory();
    else
      dir = Files.absolute(startParentDirectoryOrNull);
    while (true) {
      File candidate = new File(dir, filename);
      if (candidate.exists())
        return candidate;
      dir = dir.getParentFile();
      if (dir == null)
        return Files.DEFAULT;
    }
  }

  /**
   * Like optFileWithinParents, but throws exception if no file found
   */
  public static File getFileWithinParents(File startParentDirectoryOrNull, String filename) {
    File file = optFileWithinParents(startParentDirectoryOrNull, filename);
    if (Files.empty(file))
      throw badArg("Cannot find file", filename, "within parent directory", startParentDirectoryOrNull,
          "current", Files.currentDirectory());
    return file;
  }

}
