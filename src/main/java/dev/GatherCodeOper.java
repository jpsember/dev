/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dev.gen.GatherCodeConfig;
import js.app.AppOper;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;

public class GatherCodeOper extends AppOper {

  @Override
  public String userCommand() {
    return "gathercode";
  }

  @Override
  public String getHelpDescription() {
    return "gathers code referenced from an executable script to a central location for later zipping";
  }

  @Override
  public GatherCodeConfig defaultArgs() {
    return GatherCodeConfig.DEFAULT_INSTANCE;
  }

  @Override
  public GatherCodeConfig config() {
    return (GatherCodeConfig) super.config();
  }

  @Override
  public void perform() {
    for (Entry<String, String> ent : config().programs().entrySet()) {
      String programName = ent.getKey();
      String mainClass = ent.getValue();
      collectScriptClasses(programName);
      generateRunScript(programName, mainClass);
    }
    writeConfig();
    if (config().generateZip()) {
      createZip();
      if (config().deleteUnzipped()) {
        deleteUnzipped();
      }
    }
  }

  private File outputDir() {
    if (mOutputDir == null) {
      mOutputDir = Files.assertNonEmpty(interpretFile(config().outputDir()), "output_dir");
      files().mkdirs(mOutputDir);
    }
    return mOutputDir;
  }

  private File classesDir() {
    if (mClassesDir == null) {
      mClassesDir = files().mkdirs(new File(outputDir(), "classes"));
    }
    return mClassesDir;
  }

  private void collectScriptClasses(String programName) {
    log("collectScriptClasses", programName);
    List<File> classList = arrayList();
    mProgramClassLists.put(programName, classList);
    File scriptFile = Files.assertExists(new File(interpretFile(config().scriptsDir()), programName));
    String txt = Files.readString(scriptFile);
    List<String> lines = split(txt, '\n');

    String javaLine = null;
    for (String s : lines) {
      if (s.startsWith("java ")) {
        checkState(javaLine == null, "multiple 'java ...' lines found");
        javaLine = s;
      }
    }
    checkState(javaLine != null, "no 'java ...' line found");

    String substr = "-classpath";
    int i = javaLine.indexOf(substr);
    checkState(i >= 0, "can't find:", quote(substr), "in:", javaLine);

    String s = javaLine.substring(i + substr.length()).trim();
    i = s.indexOf(' ');
    checkState(i > 0, "can't find space in:", quote(s));
    s = s.substring(0, i);

    lines = split(s, ':');
    for (String s2 : lines) {
      if (s2.startsWith("$MVN")) {
        s2 = new File(mavenRepoDir(), chompPrefix(s2, "$MVN/")).toString();
      }
      File jarFile = new File(s2);
      File dest = copyClassesFile(jarFile);
      classList.add(dest);
    }
  }

  private File interpretFile(File file) {
    Files.assertNonEmpty(file, "can't interpret empty file");
    String s = file.toString();
    String s2 = chompPrefix(s, "~");
    if (s != s2)
      return new File(Files.homeDirectory(), s2);
    return file;
  }

  private File mavenRepoDir() {
    if (mMaven == null) {
      File mvnDir = new File(Files.homeDirectory(), ".m2/repository");
      Files.assertDirectoryExists(mvnDir, "can't find maven directory");
      mMaven = mvnDir;
    }
    return mMaven;
  }

  private File copyClassesFile(File sourceFile) {
    Files.assertExists(sourceFile, "copyFile argument");

    // Determine subdirectory to place it within
    String subdir = sourceFile.getParent().toString();
    String path = sourceFile.toString();
    String mvn = mavenRepoDir().toString();
    if (path.startsWith(mvn)) {
      subdir = chompPrefix(path, mvn + "/");
    }

    String outputSubdir = "";
    if (nonEmpty(subdir)) {
      outputSubdir = subdir;
    }
    String outputFile = sourceFile.getName();
    if (nonEmpty(outputSubdir))
      outputFile = outputSubdir + "/" + outputFile;
    File target = new File(classesDir(), outputFile);
    long sourceTime = sourceFile.lastModified();
    if (!(target.exists() && target.lastModified() >= sourceTime)) {
      log("copying", outputFile);
      files().copyFile(sourceFile, target, true);
    }
    return target;
  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), resourceName);
  }

  private void generateRunScript(String programName, String mainClass) {
    JSMap m = map();
    m.put("program_name", programName);
    m.put("main_class", mainClass);
    List<File> classFiles = mProgramClassLists.get(programName);
    {
      StringBuilder sb = new StringBuilder();
      String outputDirPrefix = classesDir().toString();
      for (File f : classFiles) {
        if (sb.length() != 0)
          sb.append(':');
        String s = f.toString();
        s = chompPrefix(s, outputDirPrefix);
        sb.append("$C");
        sb.append(s);
      }
      m.put("class_path", sb.toString());
    }
    MacroParser parser = new MacroParser();
    parser.withTemplate(frag("gather_driver_template.txt")).withMapper(m);
    String script = parser.content();
    File dest = new File(outputDir(), programName + ".sh");
    files().writeString(dest, script);
    files().chmod(dest, 744); // But: these permissions will not be preserved by zipping
  }

  private void writeConfig() {
    File target = new File(outputDir(), "params.json");
    files().writePretty(target, config());
  }

  private void createZip() {
    try {
      File zipFile = Files.setExtension(outputDir(), Files.EXT_ZIP);
      files().deletePeacefully(zipFile);
      ZipOutputStream zipOut = new ZipOutputStream(files().outputStream(zipFile));
      DirWalk d = new DirWalk(outputDir());
      String parentName = outputDir().getName();
      d.withRecurse(true);
      for (File f : d.filesRelative()) {
        byte[] bytes = Files.toByteArray(d.abs(f), "zipping");
        ZipEntry zipEntry = new ZipEntry(parentName + "/" + f.toString());
        zipOut.putNextEntry(zipEntry);
        zipOut.write(bytes);
        zipOut.closeEntry();
      }
      zipOut.close();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  private void deleteUnzipped() {
    files().deleteDirectory(outputDir(), "output");
  }

  private Map<String, List<File>> mProgramClassLists = hashMap();
  private File mOutputDir;
  private File mClassesDir;
  private File mMaven;
}
