package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.NewDatConfig;
import js.app.AppOper;
import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;

public class NewDatOper extends AppOper {

  @Override
  public String userCommand() {
    return "newdat";
  }

  @Override
  public String shortHelp() {
    return "generate a new dat file";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    b.pr("name <gen.xyz.foo.dat>");
  }

  @Override
  public NewDatConfig defaultArgs() {
    return NewDatConfig.DEFAULT_INSTANCE;
  }

  @Override
  public NewDatConfig config() {
    if (mConfig == null) {
      mConfig = (NewDatConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    File datf = Files.getFileWithinParents(null, "dat_files", "looking for dat_files directory");

    // If there is no package information in the name, derive it
    String name = config().name();
    if (false && alert("setting test name"))
      name = "alpha/bravo/frodo_baggins.java";
    if (name.isEmpty()) {
      setError("Please specify a name");
    }
    name = chomp(name, ".dat");
    name = chomp(name, ".java");
    String basename = Files.basename(name);
    name = chomp(name, basename) + DataUtil.convertCamelCaseToUnderscores(basename);

    if (!name.contains(".")) {
      DirWalk w = new DirWalk(datf).withExtensions("dat");
      File shortestSubdir = null;
      int shortestPathComponentCount = 0;
      for (File c : w.files()) {
        File dir = c.getParentFile();
        String dirStr = dir.toString();
        int pathComponents = 0;
        int i = 0;
        while (true) {
          i = dirStr.indexOf('/', i);
          if (i < 0)
            break;
          i++;
          pathComponents++;
        }
        if (shortestSubdir == null || pathComponents < shortestPathComponentCount) {
          shortestPathComponentCount = pathComponents;
          shortestSubdir = dir;
        }
      }
      if (shortestSubdir == null) {
        log("can't find any existing .dat files");
        shortestSubdir = new File(datf, "gen");
      }
      File rel = Files.relativeToContainingDirectory(shortestSubdir, datf);
      File parent = Files.join(datf, rel);
      File target = Files.join(parent, name + ".dat");
      if (target.exists())
        setError("target file already exists:", target);
      files().writeString(target, "// deprecated until you define some fields\n- class {}");
    }

  }

  private NewDatConfig mConfig;

}
