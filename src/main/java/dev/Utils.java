package dev;

import static js.base.Tools.*;

import java.io.File;

import js.base.BasePrinter;
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
   * @param errorContext
   *          if nonempty, and file isn't found, throws exception, displaying a
   *          message with these arguments
   * @return found file, or Files.DEFAULT
   */
  public static File getFileWithinParents(File startParentDirectoryOrNull, String filename,
      Object... errorContext) {
    File startDir;
    if (startParentDirectoryOrNull == null)
      startDir = Files.currentDirectory();
    else
      startDir = Files.absolute(startParentDirectoryOrNull);
    File result = Files.DEFAULT;
    File dir = startDir;
    while (true) {
      File candidate = new File(dir, filename);
      if (candidate.exists()) {
        result = candidate;
        break;
      }
      dir = dir.getParentFile();
      if (dir == null)
        break;
    }
    if (Files.empty(result) && errorContext.length > 0)
      throw badArg("Cannot find file", filename, "within", startDir, "; context:",
          BasePrinter.toString(errorContext));
    return result;
  }

}
