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
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dev.gen.DeployInfo;
import dev.gen.GatherCodeConfig;
import dev.gen.InstallFileEntry;
import dev.gen.OthersState;
import js.app.AppOper;
import js.data.DataUtil;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.MacroParser;
import js.parsing.RegExp;

public class GatherCodeOper extends AppOper {

  private File mProjectDirectory;

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
    if (mConfig == null) {
      mConfig = (GatherCodeConfig) super.config();
    }
    return mConfig;
  }

  private GatherCodeConfig mConfig;

  @Override
  public void perform() {
    if (false) {
      experiment();
      return;
    }

    mDeployInfo = DeployInfo.newBuilder().version(config().versionNumber());

    prepareVariables();

    openZip();

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

    // Write info 
    mZip.addEntry("deploy_info.json", mDeployInfo);

    closeZip();

    log("deploy_info:",INDENT,mDeployInfo);
  }

  private void prepareVariables() {
    mVarMap = hashMap();
    mVarMap.putAll(config().variables());

    File projDir = config().projectDirectory();
    if (Files.empty(projDir))
      projDir = files().projectDirectory();
    mProjectDirectory = Files.assertDirectoryExists(Files.absolute(projDir), "project directory");
    provideVar("project", mProjectDirectory);
    provideVar("home", Files.homeDirectory());

    // Apply variable substitution to the entire config object now that we've defined the variables
    String s = DataUtil.toString(config());
    s = applyVariableSubstitution(s);
    mConfig = config().parse(new JSMap(s));
  }

  private void provideVar(String key, Object obj) {
    String value = obj.toString();
    String existing = mVarMap.get(key);
    if (existing != null) {
      log("provide variable", key, "currently:", existing, "not replacing with", value);
      return;
    }
    log("provide variable", key, "=>", value);
    mVarMap.put(key, value);
  }

  private void collectProgramClasses(String programName) {
    log("collectScriptClasses", programName);
    List<String> classList = arrayList();
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
      classList.add(copyClassesFile(jarFile));
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

  private String copyClassesFile(File sourceFile) {
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
    String name = "classes/" + outputFile;
    // If this class file has already been added, don't do it again
    if (!mZip.contains(name)) {
      mZip.addEntry(name, sourceFile);
    }
    return name;
  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), resourceName);
  }

  private void generateRunScript(String programName, String mainClass) {
    JSMap m = map();
    m.put("program_name", programName);
    m.put("main_class", mainClass);
    List<String> classFiles = mProgramClassLists.get(programName);
    {
      StringBuilder sb = new StringBuilder();
      String outputDirPrefix = "classes"; //mClassesDir.toString();
      for (String s : classFiles) {
        if (sb.length() != 0)
          sb.append(':');
        s = chompPrefix(s, outputDirPrefix);
        sb.append("$C");
        sb.append(s);
      }
      m.put("class_path", sb.toString());
    }
    MacroParser parser = new MacroParser();
    parser.withTemplate(frag("gather_driver_template.txt")).withMapper(m);
    String script = parser.content();
    mZip.addEntry("programs/" + programName + ".sh", script);
  }

  private void writeConfig() {
    mZip.addEntry("params.json", config());
  }

  private void writeSecrets() {
    File source = config().secretsSource();
    if (Files.empty(source))
      return;
    Files.assertDirectoryExists(source, "secrets_source");
    File tempZipFile = Files.createTempFile("writeSecrets", ".zip");
    Zipper z = new Zipper(files());
    z.open(tempZipFile);
    for (File f : config().secretFiles()) {
      String name = f.toString();
      File sourceFile = Files.assertExists(new File(source, name), "file within secrets");
      log("...adding secret:", name);
      z.addEntry(name, sourceFile);
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
    mZip.addEntry("secrets.bin", encrypted);
    tempZipFile.delete();
  }

  private void writeOthers() {
    log("writeOthers");
    mFileEntries = hashMap();
    mCreateDirEntries = treeMap();

    File sourceDir = mProjectDirectory;

    JSList lst = config().othersList();

    lst = new JSList(applyVariableSubstitution(lst.toString()));

    OthersState state = OthersState.newBuilder() //
        .sourceDir(sourceDir) //
        .targetDir(new File("$[target]")) //
        .build();
    rewriteFileEntries(lst, state);

    // Process the rewritten list

    List<String> sortedKeys = arrayList();
    Set<String> vars = new TreeSet<String>();

    sortedKeys.addAll(mFileEntries.keySet());
    sortedKeys.sort(null);
    List<InstallFileEntry> ents = arrayList();
    for (String key : sortedKeys) {
      InstallFileEntry ent = mFileEntries.get(key);
      copyOther(ent);
      ents.add(ent);
      extractVars(ent.targetPath().toString(), vars);
    }
    List<String> asList = arrayList();
    asList.addAll(vars);

    List<InstallFileEntry> created = arrayList();
    created.addAll(mCreateDirEntries.values());

    mDeployInfo //
        .others(ents) //
        .variables(asList) //
        .createDirs(created) //
    ;
  }

  // Variables used by the compiler have the form ${ ... }
  private static final Pattern SOURCE_PATTERN_EXPRESSION = RegExp.pattern("\\$\\{\\w+\\}");
  // Variables used by installer have the form $[ ... ]
  private static final Pattern TARGET_PATTERN_EXPRESSION = RegExp.pattern("\\$\\[\\w+\\]");

  private String applyVariableSubstitution(String content) {
    Matcher m = SOURCE_PATTERN_EXPRESSION.matcher(content);

    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (m.find()) {
      String macro = m.group();

      int i2 = m.start();
      if (i2 > i) {
        sb.append(content.substring(i, i2));
        i = m.end();
      }

      String macroId = varTextPortion(macro);
      String replacement = mVarMap.get(macroId);
      if (replacement == null) {
        replacement = macro;
        badArg("undefined variable:", macro, macroId, mVarMap);
      }
      sb.append(replacement);
    }
    int i2 = content.length();
    if (i2 > i) {
      sb.append(content.substring(i));
    }
    return sb.toString();
  }

  // This assumes the first two and last one character are delimiters, e.g. "${", "}"
  //
  private String varTextPortion(String macro) {
    return macro.substring(2, macro.length() - 1);
  }

  private void extractVars(String expr, Set<String> vars) {
    Matcher m = TARGET_PATTERN_EXPRESSION.matcher(expr);
    while (m.find()) {
      vars.add(varTextPortion(m.group()));
    }
  }

  private void copyOther(InstallFileEntry ent) {
    // do it early so it becomes an absolute path
    log("copying:", INDENT, ent);
    File src = ent.sourcePath();
    checkArgument(Files.nonEmpty(src));
    String s = src.toString();
    checkArgument(!s.contains("[") && !s.contains("~"), ent);
    Files.assertExists(src, "copyOther");
    mZip.addEntry(othersSubdir(ent.key()), ent.sourcePath());
  }

  private String othersSubdir(String path) {
    return "others/" + path;
  }

  private InstallFileEntry.Builder builderWithKey(File sourceFile) {
    InstallFileEntry.Builder b = InstallFileEntry.newBuilder() //
        .sourcePath(sourceFile);
    int i = 0;
    String baseKey = sourceFile.getName();
    String key = baseKey;
    while (mFileEntries.containsKey(key)) {
      i++;
      key = baseKey + "_" + i;
      todo("insert number before extension if any?");
    }
    b.key(key);
    mFileEntries.put(key, b);
    return b;
  }

  private void rewriteFileEntries(Object fileSet, OthersState state) {
    if (fileSet instanceof String) {
      String filePath = (String) fileSet;
      File sourceFile = extendFile(state.sourceDir(), filePath);

      // If this is a directory, process recursively
      if (sourceFile.isDirectory()) {
        state = state.toBuilder().sourceDir(sourceFile).build();
        log("...writing dir:", sourceFile);
        DirWalk w = new DirWalk(state.sourceDir()).withRecurse(true).omitNames(".DS_Store");
        for (File f : w.files()) {
          rewriteFileEntries(f.getName(), state);
        }
      } else {
        InstallFileEntry.Builder b = builderWithKey(sourceFile);
        applyMissingFields(b, state);
      }
    } else if (fileSet instanceof JSList) {
      JSList fileSets = (JSList) fileSet;
      for (Object fs : fileSets.wrappedList()) {
        rewriteFileEntries(fs, state);
      }
    } else if (fileSet instanceof JSMap) {
      JSMap m = (JSMap) fileSet;
      String sourceExpr = m.opt("source", "");
      String targetDirExpr = m.opt("targetdir", "");
      boolean createDirFlag = m.opt("create");

      if (nonEmpty(targetDirExpr)) {
        state = state.toBuilder().targetDir(extendFile(state.targetDir(), targetDirExpr)).build();
      }

      Object auxFileSet = m.optUnsafe("items");

      if (createDirFlag) {
        checkState(nonEmpty(targetDirExpr));
        checkState(auxFileSet == null);
        checkState(!mCreateDirEntries.containsKey(state.targetDir()));
        InstallFileEntry.Builder b = InstallFileEntry.newBuilder() //
            .targetPath(state.targetDir());
        mCreateDirEntries.put(state.targetDir(), b);
        pr(DASHES, CR, "creating dir:", b);
        return;
      }

      if (auxFileSet == null) {
        // The name is included in sourceExpr
        checkArgument(nonEmpty(sourceExpr), "probably shouldn't be empty");
        File sourceFile = extendFile(state.sourceDir(), sourceExpr);
        InstallFileEntry.Builder b = builderWithKey(sourceFile);
        applyMissingFields(b, state);
      } else {
        state = state.toBuilder().sourceDir(extendFile(state.sourceDir(), sourceExpr)).build();
        rewriteFileEntries(auxFileSet, state);
      }
    } else
      throw notSupported("don't know how to handle fileset:", INDENT, fileSet);
  }

  private void applyMissingFields(InstallFileEntry.Builder b, OthersState state) {
    if (nullOrEmpty(b.targetName())) {
      b.targetName(b.sourcePath().getName());
    }
    if (Files.empty(b.targetPath())) {
      b.targetPath(new File(state.targetDir(), b.targetName()));
    }
  }

  // file       suffix      new
  // ---------------------------------------------
  //            abc         abc
  //            /abc        /abc
  // abc                    abc                
  // abc        xyz/def     abc/xyz/def
  // abc        /xyz/def    /xyz/def
  //
  private File extendFile(File file, String suffix) {
    File result;
    if (Files.empty(file)) {
      checkArgument(nonEmpty(suffix));
      result = new File(suffix);
    } else if (suffix.isEmpty())
      result = file;
    else if (suffix.startsWith("/"))
      result = new File(suffix);
    else
      result = new File(file, suffix);
    if (false)
      log("extendRoot:", file, "with:", suffix, INDENT, spaces(26), result);
    return result;
  }

  private Map<String, List<String>> mProgramClassLists = hashMap();
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

  private void openZip() {
    checkState(mZip == null);
    File output = config().output();
    if (Files.empty(output) || !Files.getExtension(output).equals(Files.EXT_ZIP)) {
      badArg("output should have a .zip extension:", output);
    }
    File zipFile = output;
    Zipper z = new Zipper(files());
    z.open(zipFile);
    mZip = z;
  }

  private void closeZip() {
    if (mZip == null)
      return;
    mZip.close();
    mZip = null;
  }

  private Zipper mZip;
  private Map<String, InstallFileEntry> mFileEntries;
  private Map<String, String> mVarMap;
  private DeployInfo.Builder mDeployInfo;
  private Map<File, InstallFileEntry> mCreateDirEntries;
}
