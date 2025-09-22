package dev;

import static js.base.Tools.*;

import dev.gen.CopyrightConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.base.DateTimeTools;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.RegExp;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

public class CopyrightOper extends AppOper {

  @Override
  public String userCommand() {
    return "copyright";
  }

  @Override
  public String shortHelp() {
    return "Inserts copyright message at top of source files";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("** this help is out of date**", "xxx");
    b.pr(hf);
    b.br();
    b.pr("Inserts copyright message at top of source files");
  }


  @Override
  public CopyrightConfig defaultArgs() {
    return CopyrightConfig.DEFAULT_INSTANCE;
  }

  @Override
  public CopyrightConfig config() {
    if (mConfig == null) {
      mConfig = super.config();
    }
    return mConfig;
  }

  private CopyrightConfig mConfig;

  @Override
  public void perform() {
    File sourceDir = config().sourceDir();
    if (Files.empty(sourceDir))
      sourceDir = Files.currentDirectory();
    Files.assertDirectoryExists(sourceDir, "source_dir");

    var ext = config().fileExtensions();
    if (ext.isEmpty()) {
      ext = "java";
    }
    var fileExts = DataUtil.toStringArray(split(ext, ','));
    defineProject(fileExts);

    DirWalk w = new DirWalk(sourceDir).withExtensions(fileExts);
    if (w.files().isEmpty())
      badState("no source files were found");

    String licenseText;
    if (Files.nonEmpty(config().copyrightTextFile())) {
      licenseText = parseLicense(Files.readString(Files.assertExists(config().copyrightTextFile())));
    } else {
      licenseText = parseLicense(frag("mit_license.txt"));
    }

    for (File sourceFile : w.files()) {
      // Omit 'gen' subdirectories, as we assume this is generated code
      String pathStr = "/" + sourceFile.toString();
      if (pathStr.contains("/gen/"))
        continue;
      log("processing:", sourceFile);
      String orig = Files.readString(sourceFile);

      String result = orig;

      var rgstr = config().headerRegEx();
      if (rgstr.isEmpty()) {
        if (mProjExt.equals("java")) {
          rgstr = "\\/\\*\\*(:?\\*|[^\\*\\/][^\\*]*\\*)*\\/\\n*";
        } else if (mProjExt.equals("rs")) {
          rgstr = "((\\/\\/[^\\n]*)?\\n)+";
        } else {
          badArg("Unsupported project extension:", mProjExt);
        }
      }

      // If there's a license at the start, delete it
      Matcher matcher = RegExp.matcher(rgstr, orig);
      if (matcher.find() && matcher.start() == 0) {
        result = result.substring(matcher.end());
      }

      if (!config().removeMessage())
        result = licenseText + "\n" + result;


      files().writeIfChanged(sourceFile, result);
    }
  }

  private void defineProject(String[] exts) {
    String ext = "";
    checkArgument(exts.length != 0, "no extensions defined");
    ext = exts[0];
    checkArgument(nonEmpty(ext), "extension is empty");
    mProjExt = ext;
  }

  private String mProjExt;

  private String parseLicense(String c) {
    List<String> lines = split(c, '\n');

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      String prefix;
      if (mProjExt.equals("java")) {
        if (i == 0)
          prefix = "/**\n * ";
        else if (i == lines.size() - 1)
          prefix = " **/";
        else
          prefix = " * ";
      } else if (mProjExt.equals("rs")) {
        prefix = "// ";
      } else throw badArg("unsupported extension");

      sb.append(prefix);
      sb.append(lines.get(i));
      sb.append('\n');
    }

    // Look for special year expression, and replace with current year
    String expr = "!!YEAR!!";
    boolean found = true;
    while (found) {
      found = false;
      int cursor = sb.indexOf(expr);
      if (cursor >= 0) {
        found = true;
        int year = DateTimeTools.zonedDateTime(System.currentTimeMillis()).getYear();
        sb.replace(cursor, cursor + expr.length(), "" + year);
      }
    }

    return sb.toString().trim();
  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), "copyright/" + resourceName);
  }

}
