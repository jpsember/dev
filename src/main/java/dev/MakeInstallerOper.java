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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.gen.DeployInfo;
import dev.gen.MakeInstallerConfig;
import dev.gen.FileEntry;
import dev.gen.FileParseState;
import js.app.AppOper;
import js.data.DataUtil;
import js.data.Encryption;
import js.file.DirWalk;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.MacroParser;
import js.parsing.RegExp;

public class MakeInstallerOper extends AppOper {

  public static final String DEBUG_KEY = "";// "start.sh" //

  @Override
  public String userCommand() {
    return "makeinstaller";
  }

  @Override
  public String getHelpDescription() {
    return "creates an installer zip file from a script";
  }

  @Override
  public MakeInstallerConfig defaultArgs() {
    return MakeInstallerConfig.DEFAULT_INSTANCE;
  }

  @Override
  public MakeInstallerConfig config() {
    if (mConfig == null) {
      mConfig = (MakeInstallerConfig) super.config();
    }
    return mConfig;
  }

  private MakeInstallerConfig mConfig;

  private List<String> mDebugKeys;

  @Override
  public void perform() {
    mDeployInfo = DeployInfo.newBuilder().version(config().versionNumber());
    {
      String debugStr = config().sourceVariables().get("-debug-");
      if (nonEmpty(debugStr)) {
        mDebugKeys = split(debugStr, ' ');
      }
    }

    prepareVariables();
    if (mDebugKeys != null) {
      writeFiles(true);
      return;
    }
    openZip();
    writeFiles(false);
    writePrograms();
    writeDeployInfo();
    writeConfig();
    closeZip();
    log("deploy_info:", INDENT, mDeployInfo);
  }

  /**
   * Process the set of programs, collecting required classes, generate run
   * scripts
   */
  private void writePrograms() {
    for (Entry<String, String> ent : config().programs().entrySet()) {
      String programName = ent.getKey();
      String mainClass = ent.getValue();
      collectProgramClasses(programName);
      generateRunScript(programName, mainClass);
    }
  }

  private void writeDeployInfo() {
    String content = mDeployInfo.toJson().prettyPrint();
    mZip.addEntry("deploy_info.json", content);
    files().writeString(new File("_SKIP_deploy_info.json"), content);
  }

  private void prepareVariables() {
    mVarMap = hashMap();
    mVarMap.putAll(config().sourceVariables());

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
    File scriptFile = Files.assertExists(new File(config().scriptsDir(), programName));
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
      String outputDirPrefix = "classes";
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
    // Strip some fields from the config that are not required by the installer
    mZip.addEntry("make_installer_config.json", config().toBuilder() //
        .secretPassphrase(null) //
        .sourceVariables(null) //
        .fileList(null) //
    );
  }

  private void writeFiles(boolean devMode) {
    log("writeFiles");
    mFileEntries = hashMap();
    mTargetMap = hashMap();
    mCreateDirEntries = treeMap();

    File sourceDir = mProjectDirectory;

    JSList jsonList = config().fileList();

    jsonList = new JSList(applyVariableSubstitution(jsonList.toString()));

    // Construct the initial FileState object
    //
    FileParseState state = FileParseState.newBuilder() //
        .sourceDir(sourceDir) //
        .targetDir(new File("$[target]")) //
        .build();

    parseFileEntries(jsonList, state);

    // Process the rewritten list
    List<String> sortedKeys = toArray(mFileEntries.keySet());
    Set<String> vars = new TreeSet<String>();
    sortedKeys.sort(null);
    if (devMode) {
      performDevMode();
      return;
    }

    List<FileEntry> ents = arrayList();

    for (String key : sortedKeys) {
      // Now that we're done joining paths together, strip out the '^' prefixes;
      FileEntry.Builder ent = mFileEntries.get(key).toBuilder();
      ent.sourcePath(resolveFile(ent.sourcePath(), false));
      ent.targetPath(resolveFile(ent.targetPath(), true));
      processFileEntry(ent);
      extractVars(ent.targetPath().toString(), vars);

      // If the permissions are unusual, add them
      {
        File f = Files.assertExists(ent.sourcePath(), "examining permissions");
        String perm = "";
        if (f.canExecute())
          perm += "x";
        ent.permissions(perm);
      }

      // Remove the source path, as it is not needed on the target
      ent.sourcePath(null);
      ents.add(ent.build());
    }

    if (mRequirePassphraseValidation)

    {
      String msg = "hello";
      byte[] clearBytes = msg.getBytes();
      byte[] encrypted = Encryption.encrypt(clearBytes, config().secretPassphrase());
      mDeployInfo.checkPassphrase(encrypted);
    }

    mDeployInfo //
        .files(ents) //
        .variables(toArray(vars)) //
        .createDirs(toArray(mCreateDirEntries.values())) //
    ;
  }

  private void performDevMode() {
    pr(DASHES);
    pr("Examining -debug- keys:", DASHES);
    JSMap m = map();
    for (String key : mDebugKeys) {
      FileEntry ent = mFileEntries.get(key);
      m.put(key, ent);
      if (ent == null)
        continue;
      pr(ent);
    }
    File targ = new File("_SKIP_FileEntries.json");
    files().writePretty(targ, m);
    int checksum = (DataUtil.checksum(targ) & 0xffff) % 9000 + 1000;
    File checksumFile = new File("_SKIP_cs.json");
    JSMap checksumMap = JSMap.fromFileIfExists(checksumFile);
    int prevChecksum = checksumMap.opt("", 0);
    if (prevChecksum != checksum) {
      pr("**** Checksum has changed to:", checksum);
      if (prevChecksum == 0) {
        checksumMap.put("", checksum);
        files().writePretty(checksumFile, checksumMap);
      }
    }
    return;
  }

  public static <T> List<T> toArray(Collection<T> collection) {
    ArrayList<T> lst = new ArrayList<>();
    lst.addAll(collection);
    return lst;
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

  /* private */ File assertResolved(File f) {
    String s = Files.assertNonEmpty(f).toString();
    checkArgument(!s.startsWith("^"), "unresolved file:", f);
    return f;
  }

  private File resolveFile(File f, boolean varAllowed) {
    String s = Files.assertNonEmpty(f).toString();
    if (s.startsWith("^")) {
      s = s.substring(1);
    }
    checkArgument(!s.contains("~"), f);
    if (!varAllowed)
      checkArgument(!s.contains("["), f);
    return new File(s);
  }

  private void processFileEntry(FileEntry ent) {
    log("copying:", INDENT, ent);
    File src = ent.sourcePath();
    Files.assertExists(src, "copyOther");
    String zipKey = "files/" + ent.key();
    if (ent.encrypt()) {
      checkNonEmpty(config().secretPassphrase(), "no secret_passphrase given");
      byte[] clearBytes = Files.toByteArray(src, "encrypting file");
      byte[] encrypted = Encryption.encrypt(clearBytes, config().secretPassphrase());
      mZip.addEntry(zipKey, encrypted);
      mRequirePassphraseValidation = true;
    } else
      mZip.addEntry(zipKey, src);
  }

  private static final String KEY_ENCRYPT = "encrypt";
  private static final String KEY_SOURCE = "source";
  private static final String KEY_TARGET = "target";
  private static final String KEY_ITEMS = "items";
  private static final String KEY_VARS = "vars";
  private static final String KEY_LIMIT = "limit";

  private void parseFileEntries(Object argument, FileParseState parentState) {
    log("parseFileEntries; state:", INDENT, parentState, CR, "arg:", CR, argument, DASHES);

    // Make sure we are dealing with an immutable FileParseState,
    // and construct a new builder from it to apply to subsequent entries
    //
    FileParseState.Builder state = parentState.build().toBuilder();

    // If File or string <x>,
    //
    // Extend both source and target by <x> and process the resulting FileParseState
    //

    if (argument instanceof File || argument instanceof String) {
      String filePath = argument.toString();
      state.sourceDir(extendFile(state.sourceDir(), filePath));
      state.targetDir(extendFile(state.targetDir(), filePath));
      todo("can we replace processFileOrDir by a base case somehow?");
      processFileOrDir(state);
      return;
    }

    // If [ <elem> ...], parse each element recursively
    //
    if (argument instanceof JSList) {
      JSList jsonList = (JSList) argument;
      for (Object fs : jsonList.wrappedList()) {
        parseFileEntries(fs, state);
      }
      return;
    }

    // This is the canonical form of argument, essentially the 'base case'
    //
    if (argument instanceof JSMap) {
      JSMap m = (JSMap) argument;
      parseFileEntry(m, state);
      return;
    }

    throw notSupported("don't know how to parse:", INDENT, argument);
  }

  private void addEntry(FileParseState state) {
    FileEntry.Builder b = FileEntry.newBuilder() //
        .sourcePath(state.sourceDir());
    int i = 0;
    String baseKey = b.sourcePath().getName();
    checkNonEmpty(baseKey, "can't extract key from source_path:", INDENT, b);

    String key = baseKey;

    // Determine root name to store file within the zip file.
    // Use the basename; if it fails to determine that, try again
    // after replacing all '.' with '_'.
    String basename = null;
    try {
      basename = Files.basename(key);
    } catch (IllegalArgumentException e) {
      basename = key.replace('.', '_');
      log("key was:", key, "basename is now:", basename);
    }

    String ext = Files.getExtension(key);
    while (mFileEntries.containsKey(key)) {
      i++;
      key = Files.setExtension(basename + "__" + i, ext);
    }
    b.key(key);
    b.encrypt(state.encrypt());
    b.vars(state.vars());

    //don't start in directory other than current?

    if (Files.empty(b.targetPath())) {
      b.targetPath(state.targetDir());
    }
    b.encrypt(state.encrypt());
    if (key.equals(DEBUG_KEY))
      die(b);
    mFileEntries.put(key, b.build());

    // If en entry with this target already exists, remove it
    String currentTargetKey = mTargetMap.get(b.targetPath());
    if (currentTargetKey != null) {
      log("replacing target with new:", b.targetPath());
      mFileEntries.remove(currentTargetKey);
    }
    mTargetMap.put(b.targetPath(), key);

    log(VERT_SP, "created file entry:", key, "=>", INDENT, b, VERT_SP);
  }

  public static void assertLegalSet(Collection<String> collection, Set<String> allowedItems) {
    for (String k : collection) {
      if (!allowedItems.contains(k)) {
        throw badArg("Illegal key in set:", k);
      }
    }
  }

  private static boolean optBool(JSMap m, String key, boolean defaultValue) {
    Boolean r = (Boolean) m.optUnsafe(key);
    if (r == null)
      r = defaultValue;
    return defaultValue;
  }

  private static Set<String> sAllowedKeys = Set.of(KEY_SOURCE, KEY_TARGET, KEY_ENCRYPT, KEY_ITEMS, KEY_VARS,
      KEY_LIMIT);

  private void parseFileEntry(JSMap m, FileParseState.Builder newState) {
    assertLegalSet(m.keySet(), sAllowedKeys);

    String sourceExpr = m.opt(KEY_SOURCE, "");
    String targetExpr = m.opt(KEY_TARGET, "");

    newState.encrypt(optBool(m, KEY_ENCRYPT, newState.encrypt()));
    newState.vars(optBool(m, KEY_VARS, newState.vars()));

    newState.sourceDir(extendFile(newState.sourceDir(), sourceExpr));
    newState.targetDir(extendFile(newState.targetDir(), targetExpr));

    int limit = m.opt(KEY_LIMIT, 0);
    newState.limit(limit);
    Object itemsExpr = m.optUnsafe(KEY_ITEMS);

    if (itemsExpr == null) {
      // If no source key given, but target has been given, create target
      //
      if (sourceExpr.isEmpty() && !targetExpr.isEmpty()) {
        checkState(!mCreateDirEntries.containsKey(newState.targetDir()));
        FileEntry.Builder b = FileEntry.newBuilder() //
            .targetPath(newState.targetDir());
        mCreateDirEntries.put(newState.targetDir(), b);
        return;
      }

      if (!sourceExpr.isEmpty()) {
        processFileOrDir(newState);
        return;
      }

      throw badArg("no items specified:", INDENT, m);
    }
    if (itemsExpr instanceof String) {
      String filename = (String) itemsExpr;
      newState //
          .sourceDir(extendFile(newState.sourceDir(), filename)) //
          .targetDir(extendFile(newState.targetDir(), filename)) //
      ;
      addEntry(newState);
      return;
    }
    if (!(itemsExpr instanceof JSList)) {
      throw badArg("unexpected 'items' argument:", INDENT, m);
    }

    parseFileEntries(itemsExpr, newState);
  }

  private void processFileOrDir(FileParseState.Builder state) {
    // If this is a directory, process recursively
    File source = resolveFile(state.sourceDir(), false);
    if (source.isDirectory()) {
      log("...writing dir:", source);
      DirWalk w = new DirWalk(source).withRecurse(true).omitNames(".DS_Store");
      List<File> files = w.filesRelative();
      if (state.limit() > 0)
        removeAllButFirstN(files, state.limit());
      for (File f : files) {
        log("...processing relative file:", f);
        parseFileEntries(f, state);
      }
    } else {
      addEntry(state);
    }
  }

  // file       suffix      new
  // ---------------------------------------------
  //            abc         abc
  //            /abc        /abc
  // abc                    abc                
  // abc        xyz/def     abc/xyz/def
  // abc        /xyz/def    /xyz/def
  // ^abc       ^xyz        ^xyz
  //
  private File extendFile(File file, String suffix) {
    File result;
    if (Files.empty(file)) {
      checkArgument(nonEmpty(suffix));
      result = new File(suffix);
    } else if (suffix.isEmpty())
      result = file;
    else if (suffix.startsWith("/") || suffix.startsWith("^"))
      result = new File(suffix);
    else
      result = new File(file, suffix);
    return result;
  }

  private Map<String, List<String>> mProgramClassLists = hashMap();
  private File mMaven;

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

  private File mProjectDirectory;
  private Zipper mZip;
  private Map<String, FileEntry> mFileEntries;
  private Map<File, String> mTargetMap;
  private Map<String, String> mVarMap;
  private DeployInfo.Builder mDeployInfo;
  private Map<File, FileEntry> mCreateDirEntries;
  private boolean mRequirePassphraseValidation;
}
