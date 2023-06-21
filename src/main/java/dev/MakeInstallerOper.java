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
import dev.gen.FileState;
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

  private File mProjectDirectory;

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

  @Override
  public void perform() {
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

    writeFiles();

    // Write info 
    mZip.addEntry("deploy_info.json", mDeployInfo);

    closeZip();

    log("deploy_info:", INDENT, mDeployInfo);
  }

  private void prepareVariables() {
    mVarMap = hashMap();
    if (config().sourceVariables() != null)
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
    // Strip some fields from the params, namely the secret passphrase, and
    // the files_list 
    mZip.addEntry("params.json", config().toBuilder() //
        .secretPassphrase(null) //
        .sourceVariables(null) //
        .fileList(null) //
    );
  }

  private void writeFiles() {
    log("writeFiles");
    mFileEntries = hashMap();
    mCreateDirEntries = treeMap();

    File sourceDir = mProjectDirectory;

    JSList jsonList = config().fileList();

    jsonList = new JSList(applyVariableSubstitution(jsonList.toString()));

    // Construct the initial FileState object
    //
    FileState state = FileState.newBuilder() //
        .sourceDir(sourceDir) //
        .targetDir(new File("$[target]")) //
        .build();

    parseFileEntries(jsonList, state);

    // Process the rewritten list

    List<String> sortedKeys = toArray(mFileEntries.keySet());
    Set<String> vars = new TreeSet<String>();
    sortedKeys.sort(null);
    List<FileEntry> ents = arrayList();
    for (String key : sortedKeys) {
      FileEntry ent = mFileEntries.get(key);
      processFileEntry(ent);
      ents.add(ent);
      extractVars(ent.targetPath().toString(), vars);
    }

    mDeployInfo //
        .files(ents) //
        .variables(toArray(vars)) //
        .createDirs(toArray(mCreateDirEntries.values())) //
    ;
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

  private void processFileEntry(FileEntry ent) {
    // do it early so it becomes an absolute path
    log("copying:", INDENT, ent);
    File src = ent.sourcePath();
    checkArgument(Files.nonEmpty(src));
    String s = src.toString();
    checkArgument(!s.contains("[") && !s.contains("~"), ent);
    Files.assertExists(src, "copyOther");
    String zipKey = "files/" + ent.key();
    if (ent.encrypt()) {
      checkNonEmpty(config().secretPassphrase(), "no secret_passphrase given");
      byte[] clearBytes = Files.toByteArray(ent.sourcePath(), "encrypting file");
      byte[] encrypted = Encryption.encrypt(clearBytes, config().secretPassphrase());
      mZip.addEntry(zipKey, encrypted);
      //halt("added encrypted value for:", ent.sourcePath(), INDENT, DataUtil.hexDump(encrypted),CR,DataUtil.hexDump(clearBytes));
    } else
      mZip.addEntry(zipKey, ent.sourcePath());
  }

  private static final String KEY_ENCRYPT = "encrypt";
  private static final String KEY_SOURCE = "source";
  private static final String KEY_TARGET = "target";
  private static final String KEY_ITEMS = "items";

  // FileEntry <f> is one of:
  //
  //   { "key" : "value" ... }
  // | "file or directory"
  // | [ <f>* ]
  //
  //
  private void parseFileEntries(Object argument, FileState state) {
    log("parseFileEntries; state:", INDENT, state, CR, "arg:", CR, argument, DASHES);
    // Make sure we are dealing with an immutable FileState,
    // and construct a new builder from it to apply to subsequent entries
    //
    state = state.build();
    FileState.Builder newState = state.toBuilder();

    // This is the canonical form of argument
    //
    if (argument instanceof JSMap) {
      JSMap m = (JSMap) argument;
      parseFileEntry(m, newState);
      return;
    }

    if (argument instanceof JSList) {
      JSList jsonList = (JSList) argument;
      for (Object fs : jsonList.wrappedList()) {
        parseFileEntries(fs, newState);
      }
      return;
    }

    if (argument instanceof String) {
      String filePath = (String) argument;
      newState.sourceDir(extendFile(newState.sourceDir(), filePath));
      newState.targetDir(extendFile(newState.targetDir(), filePath));
      // If this is a directory, process recursively
      if (newState.sourceDir().isDirectory()) {
        log("...writing dir:", newState.sourceDir());
        DirWalk w = new DirWalk(newState.sourceDir()).withRecurse(true).omitNames(".DS_Store");
        for (File f : w.files()) {
          parseFileEntries(f.getName(), newState);
        }
      } else {
        addEntry(newState);
      }
      return;
    }

    throw notSupported("don't know how to parse:", INDENT, argument);
  }

  private void addEntry(FileState state) {
    FileEntry.Builder b = FileEntry.newBuilder() //
        .sourcePath(state.sourceDir());
    int i = 0;
    String baseKey = b.sourcePath().getName();
    checkNonEmpty(baseKey, "can't extract key from source_path:", INDENT, b);
    String key = baseKey;
    String basename = Files.basename(key);
    String ext = Files.getExtension(key);
    while (mFileEntries.containsKey(key)) {
      i++;
      key = Files.setExtension(basename + "__" + i, ext);
    }
    b.key(key);
    b.encrypt(state.encrypt());

    if (Files.empty(b.targetPath())) {
      b.targetPath(state.targetDir());
    }
    b.encrypt(state.encrypt());
    mFileEntries.put(key, b);
    log(VERT_SP, "created file entry:", key, "=>", INDENT, b, VERT_SP);
  }

  public static void assertLegalSet(Collection<String> collection, Set<String> allowedItems) {
    for (String k : collection) {
      if (!allowedItems.contains(k)) {
        throw badArg("Illegal key in set:", k);
      }
    }
  }

  public static int countUniqueKeys(Collection<String> collection, Set<String> keys) {
    Set<String> uniqueCollection = hashSet();
    uniqueCollection.addAll(collection);
    uniqueCollection.retainAll(keys);
    return uniqueCollection.size();
  }

  private static Set<String> sAllowedKeys = Set.of(KEY_SOURCE, KEY_TARGET, KEY_ENCRYPT, KEY_ITEMS);
  private static Set<String> sExclusiveKeys = Set.of(KEY_ENCRYPT);

  private void parseFileEntry(JSMap m, FileState.Builder newState) {
    assertLegalSet(m.keySet(), sAllowedKeys);
    // Not necessary at present, as there is only one key in the exclusive set:
    if (countUniqueKeys(m.keySet(), sExclusiveKeys) > 1)
      badArg("violation of mutually exclusive options:", INDENT, m);

    String sourceExpr = m.opt(KEY_SOURCE, "");
    String targetExpr = m.opt(KEY_TARGET, "");

    newState.encrypt(m.opt(KEY_ENCRYPT));

    // If has "target" key, update target_dir
    //
    if (nonEmpty(targetExpr))
      newState.targetDir(extendFile(newState.targetDir(), targetExpr));
    else
      newState.targetDir(extendFile(newState.targetDir(), sourceExpr));

    newState.sourceDir(extendFile(newState.sourceDir(), sourceExpr));

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
      throw badArg("no items specified:", INDENT, m);
    }
    if (itemsExpr instanceof String) {
      String filename = (String) itemsExpr;
      newState.sourceDir(extendFile(newState.sourceDir(), filename));
      addEntry(newState);
      return;
    }
    if (!(itemsExpr instanceof JSList)) {
      throw badArg("unexpected 'items' argument:", INDENT, m);
    }
    parseFileEntries(itemsExpr, newState);
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

  private Zipper mZip;
  private Map<String, FileEntry> mFileEntries;
  private Map<String, String> mVarMap;
  private DeployInfo.Builder mDeployInfo;
  private Map<File, FileEntry> mCreateDirEntries;
}
