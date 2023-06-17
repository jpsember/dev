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
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dev.gen.GatherCodeConfig;
import dev.gen.InstallFileEntry;
import js.app.AppOper;
import js.data.AbstractData;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSList;
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
    if (false) {
      experiment();
      return;
    }

    writeConfig();

    // Process the set of programs, collecting required classes, generate run scripts
    for (Entry<String, String> ent : config().programs().entrySet()) {
      String programName = ent.getKey();
      String mainClass = ent.getValue();
      collectProgramClasses(programName);
      generateRunScript(programName, mainClass);
    }

    writeSecrets();

    writeOthers();

    if (config().generateZip()) {
      createZip();
      if (config().deleteUnzipped()) {
        deleteUnzipped();
      }
    }

    todo("ability to create directories (without copying things to them)");
  }

  private File outputDir() {
    if (mOutputDir == null) {
      mOutputDir = interpretFile(config().outputDir(), "output_dir");
      files().remakeDirs(mOutputDir);
      mClassesDir = files().mkdirs(outputFile("classes"));
      mProgramsDir = files().mkdirs(outputFile("programs"));
      mOthersDir = files().mkdirs(outputFile("others"));
    }
    return mOutputDir;
  }

  private void collectProgramClasses(String programName) {
    log("collectScriptClasses", programName);
    List<File> classList = arrayList();
    mProgramClassLists.put(programName, classList);
    File scriptFile = Files
        .assertExists(new File(interpretFile(config().scriptsDir(), "scripts_dir"), programName));
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

  /**
   * Convert a file ~.... as being in the current directory;
   * 
   * Return an absolute form of the resulting file
   * 
   */
  private File interpretFile(File file, String context) {
    Files.assertNonEmpty(file, context);
    String s = file.toString();
    File result = file;

    do {
      String prefix = "~";
      if (s.startsWith(prefix)) {
        result = new File(Files.homeDirectory(), chompPrefix(s, prefix));
        break;
      }
    } while (false);

    result = result.getAbsoluteFile();
    log("interpret:", file, "=>", result);
    return result;
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
    File target = new File(mClassesDir, outputFile);
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
      String outputDirPrefix = mClassesDir.toString();
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
    File dest = new File(mProgramsDir, programName + ".sh");
    files().writeString(dest, script);
  }

  private void writeConfig() {
    files().writePretty(outputFile("params.json"), config());
  }

  private File outputFile(String path) {
    return new File(outputDir(), path);
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
      z.addEntry(sourceFile, f.toString());
    }
    z.close();
  }

  private void deleteUnzipped() {
    files().deleteDirectory(outputDir(), outputDir().getName());
  }

  private void writeSecrets() {
    File source = config().secretsSource();
    if (Files.empty(source))
      return;
    Files.assertDirectoryExists(source, "secrets_source");
    File tempZipFile = Files.createTempFile("writeSecrets", ".zip");
    Zipper z = new Zipper(files());
    z.openForWriting(tempZipFile);
    for (File f : config().secretFiles()) {
      String name = f.toString();
      File sourceFile = Files.assertExists(new File(source, name), "file within secrets");
      log("...adding secret:", name);
      z.addEntry(sourceFile, name);
    }
    z.close();

    // Encrypt the secrets zip file
    byte[] bytes = Files.toByteArray(tempZipFile, "zip file before encryption");

    byte[] encrypted;
    if (todo("add support for Java and Go compatible encryption"))
      encrypted = bytes;
    else {
      checkArgument(nonEmpty(config().secretPassphrase()), "no secret_passphrase");
      encrypted = SecretsOper.encryptData(config().secretPassphrase(), bytes);
    }
    if (todo("add support for Java and Go compatible encryption"))
      encrypted = bytes;
    files().write(encrypted, outputFile("secrets.bin"));
    tempZipFile.delete();
  }

  private void writeOthers() {
    log("writeOthers");
    mFileEntries = hashMap();
    File root = config().projectRoot();
    if (Files.empty(root))
      root = files().projectDirectory();
    root = interpretFile(root, "project_root");
    Files.assertDirectoryExists(root, "project_root");

    JSList lst = config().othersList();

    InstallFileEntry b = InstallFileEntry.newBuilder().sourcePath(root).build();

    auxWriteOthers(b, lst);

    // Write the script

    JSMap m = map();
    for (InstallFileEntry ent : mFileEntries.values())
      m.put(ent.sourceName(), ent.toJson());
    files().writePretty(outputFile("others_info.json"), m);
    log("others_info.json:", INDENT, m);
  }

  private void auxWriteOthers(InstallFileEntry fileEnt, Object fileSet) {
    assertBuilt(fileEnt);
    if (fileSet instanceof String) {
      InstallFileEntry b = parseInstallExpr(fileEnt, (String) fileSet);
      othersCopy(b);
    } else if (fileSet instanceof JSList) {
      JSList fileSets = (JSList) fileSet;
      for (Object fs : fileSets.wrappedList()) {
        auxWriteOthers(fileEnt, fs);
      }
    } else if (fileSet instanceof JSMap) {
      JSMap m = (JSMap) fileSet;
      String auxRoot = m.opt("source", "");
      Object auxFileSet = m.optUnsafe("items");
      checkArgument(auxFileSet != null, "expected items:", INDENT, m);
      auxWriteOthers(extendRoot(fileEnt, auxRoot), auxFileSet);
    } else
      throw notSupported("don't know how to handle fileset:", INDENT, fileSet);
  }

  private InstallFileEntry parseInstallExpr(InstallFileEntry ent, String filenameExpr) {
    ent = extendRoot(ent, filenameExpr);
    //b.sourcePath(ext);

    todo("how to handle specifying directories on install system?");
    // b.targetPath(ext);
    return ent;
  }

  // root       aux         new root
  // ---------------------------------------------
  // abc                    abc                
  // abc        xyz/def     abc/xyz/def
  // abc        ~           <current directory>
  // abc        ~/alpha     <current directory>/alpha
  //
  private InstallFileEntry extendRoot(InstallFileEntry ent, String aux) {
    assertBuilt(ent);
    if (aux.isEmpty())
      return ent;

    if (aux.equals("~"))
      aux = "~/";
    if (aux.startsWith("~/")) {
      String remaining = aux.substring(2);
      File newRoot = Files.currentDirectory();
      if (nonEmpty(remaining)) {
        newRoot = new File(newRoot, remaining);
      }
      return ent.toBuilder().sourcePath(newRoot).build();
    } else if (aux.startsWith("/")) {
      throw badArg("unsupported source directory argument:", aux);
    } else
      return ent.toBuilder().sourcePath(new File(ent.sourcePath(), aux)).build();
  }

  int k;

  private void assertBuilt(AbstractData data) {
    checkState(data.build() == data, "was not built");
  }

  private void othersCopy(InstallFileEntry b) {
    assertBuilt(b);
    log("othersCopy", INDENT, b);
    File sourceFile = b.sourcePath();
    if (!sourceFile.exists()) {
      badArg("file doesn't exist:", sourceFile, INDENT, b);
    }
    if (sourceFile.isDirectory()) {
      log("...writing dir:", sourceFile);
      DirWalk w = new DirWalk(sourceFile).withRecurse(true).omitNames(".DS_Store");
      for (File f : w.files()) {
        InstallFileEntry child = parseInstallExpr(b, f.getName());
        othersCopy(child);
      }
    } else {
      // Find a unique name to store this within the zip file.
      String keyStart = b.sourcePath().getName();
      String key = keyStart;
      int i = 0;
      while (mFileEntries.containsKey(key)) {
        i++;
        key = keyStart + "_" + i;
      }
      b = b.toBuilder().sourceName(key).build();
      mFileEntries.put(b.sourceName(), b);

      files().copyFile(sourceFile, new File(mOthersDir, b.sourceName()));
    }
  }

  private Map<String, List<File>> mProgramClassLists = hashMap();
  private File mOutputDir;
  private File mClassesDir;
  private File mProgramsDir;
  private File mOthersDir;
  private File mMaven;

  private static Cipher cipher;

  public static void experiment() {

    // https://stackoverflow.com/questions/55370699/getting-different-result-cyphertext-while-using-aes-in-java-and-golang

    try {
      Key secretKey;
      secretKey = (Key) new SecretKeySpec("0123456789012345".getBytes(), "AES");

      cipher = Cipher.getInstance("AES/GCM/NoPadding");

      String plainText = "The time has come the walrus said to talk of many things";
      System.out.println("Plain Text Before Encryption: " + plainText);
      String encryptedText = encrypt(plainText, secretKey);
      System.out.println("Encrypted Text After Encryption: " + encryptedText);
      String decryptedText = decrypt(encryptedText, secretKey);
      System.out.println("Decrypted Text After Decryption: " + decryptedText);
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  public static String encrypt(String plainText, Key secretKey) throws Exception {
    byte[] plainTextByte = plainText.getBytes();

    byte[] iv = new byte[12];
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

    byte[] encryptedByte = cipher.doFinal(plainTextByte);
    Base64.Encoder encoder = Base64.getEncoder();
    String encryptedText = encoder.encodeToString(encryptedByte);
    return encryptedText;
  }

  public static String decrypt(String encryptedText, Key secretKey) throws Exception {
    Base64.Decoder decoder = Base64.getDecoder();
    byte[] encryptedTextByte = decoder.decode(encryptedText);

    byte[] iv = new byte[12];
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

    byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
    String decryptedText = new String(decryptedByte);
    return decryptedText;
  }

  private Map<String, InstallFileEntry> mFileEntries;
}
