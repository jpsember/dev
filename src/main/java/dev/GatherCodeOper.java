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
    copySecrets();
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
      files().remakeDirs(mOutputDir);
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
      file = new File(Files.homeDirectory(), s2);
    return file.getAbsoluteFile();
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
  }

  private void writeConfig() {
    File target = new File(outputDir(), "params.json");
    files().writePretty(target, config());
  }

  private static class Zipper {

    public Zipper(Files f) {
      if (f == null)
        f = Files.S;
      mFiles = f;
    }

    public void openForWriting(File zipFile) {
      checkState(mZipFile == null);
      checkArgument(Files.getExtension(Files.assertNonEmpty(zipFile, "zipFile arg")).equals(Files.EXT_ZIP),
          zipFile, "not a zip file");
      mZipFile = zipFile;
      mFiles.deletePeacefully(zipFile);
      mOutputStream = new ZipOutputStream(mFiles.outputStream(zipFile));
    }

    public void addEntry(File file, String name) {
      checkState(mOutputStream != null);
      byte[] bytes = Files.toByteArray(file, "zipping");
      if (nullOrEmpty(name))
        name = file.toString();

      ZipEntry zipEntry = new ZipEntry(name);
      try {
        mOutputStream.putNextEntry(zipEntry);
        mOutputStream.write(bytes);
        mOutputStream.closeEntry();
      } catch (IOException e) {
        throw Files.asFileException(e);
      }
    }

    public void close() {
      checkState(mOutputStream != null);
      try {
        mOutputStream.close();
      } catch (IOException e) {
        throw Files.asFileException(e);
      }
      mOutputStream = null;
    }

    private final Files mFiles;
    private File mZipFile;
    private ZipOutputStream mOutputStream;
  }

  private void createZip() {
    File zipFile = Files.setExtension(outputDir(), Files.EXT_ZIP);
    Zipper z = new Zipper(files());
    z.openForWriting(zipFile);
    DirWalk d = new DirWalk(outputDir());
    d.withRecurse(true);
    for (File f : d.filesRelative()) {
      File sourceFile = d.abs(f);
      z.addEntry(sourceFile, null);
    }
    z.close();
  }

  private void deleteUnzipped() {
    files().deleteDirectory(outputDir(), outputDir().getName());
  }

  private void copySecrets() {
    File source = config().secretsSource();
    if (Files.empty(source))
      return;
    Files.assertDirectoryExists(source, "secrets_source");

    File targetFile = new File(outputDir(), "secrets.zip");
    Zipper z = new Zipper(files());
    z.openForWriting(targetFile);
    for (File f : config().secretFiles()) {
      String name = f.toString();
      File sourceFile = Files.assertExists(new File(source, name), "file within secrets");
      log("...adding secret:", name);
      z.addEntry(sourceFile, name);

    }
    z.close();

    // Encrypt the secrets zip file
    todo("encrypt secrets");
  }

  private Map<String, List<File>> mProgramClassLists = hashMap();
  private File mOutputDir;
  private File mClassesDir;
  private File mMaven;
}
