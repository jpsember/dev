package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dev.gen.GetRepoCache;
import dev.gen.GetRepoConfig;
import dev.gen.GetRepoEntry;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.FileException;
import js.file.Files;

public class GetRepoOper extends AppOper {

  @Override
  public String userCommand() {
    todo("update the help to reflect the args file change");
    return "getrepo";
  }

  @Override
  public String shortHelp() {
    return "install GitHub projects as local Maven dependencies";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem(" name <user/repo>", "GitHub repository");
    hf.addItem("[ hash <hhhhhhh> ]", "commit hash");
    hf.addItem("[ version <x> ]", "version number to assign to Maven dependency");
    b.pr(hf);
    b.br();
    b.pr("If no hash and version number are given, uses the most recent commit with version = '"
        + LATEST_COMMIT_NAME + "'.");
  }

  @Override
  public GetRepoConfig defaultArgs() {
    return GetRepoConfig.DEFAULT_INSTANCE;
  }

  @Override
  public GetRepoConfig config() {
    if (mConfig == null) {
      mConfig = (GetRepoConfig) super.config();
    }
    return mConfig;
  }

  private GetRepoConfig mConfig;

  @Override
  public void perform() {

    var entries = config().entries();

    if (entries.isEmpty())
      log("...no entries found in config file");
    Set<String> processed = hashSet();
    for (var ent : entries) {
      var repoName = ent.repoName();
      if (!processed.add(repoName))
        badArg("duplicate repo_name:", repoName);
      processEntry(ent);
    }

    if (!config().eclipse())
      discardWorkDirectory();
    flushCache();
  }

  private void processEntry(GetRepoEntry entry) {
    log("processing entry:", INDENT, entry);
    var repoName = entry.repoName();
    if (nullOrEmpty(repoName))
      setError("Illegal repo name:", INDENT, entry);
    repoName = chomp(repoName, ".git");

    var hash = entry.commitHash().toLowerCase();
    var versionNumber = entry.version();

    if (nullOrEmpty(hash) && nullOrEmpty(versionNumber)) {
      versionNumber = LATEST_COMMIT_NAME;
    } else {
      checkState(!nullOrEmpty(hash), "supply an explicit commit hash");
      checkState(!nullOrEmpty(versionNumber) && !versionNumber.equals(LATEST_COMMIT_NAME),
          "please specify a version to assign to commit", hash);
    }
    readRepo(repoName, hash, versionNumber);
  }

  private void readRepo(String repoName, String commitHash, String versionNumber) {

    log("reading repo", repoName, "commit", commitHash, "to install locally as version", versionNumber);

    mRepoName = repoName;
    mCommitHash = commitHash;
    mVersionNumber = versionNumber;

    var mp = readCache().repoMap().optJSMapOrEmpty(repoName);

    String existingVersion = mp.opt(commitHash, "");
    if (!nullOrEmpty(existingVersion)) {
      checkState(existingVersion.equals(versionNumber), "Repo", repoName, "commit", commitHash,
          "exists in repo but with a different version number:", existingVersion);
      log("...already installed");
      return;
    }

    cloneRepo();
    checkoutDesiredCommit();
    modifyPom();
    installRepoToLocalRepository();

    // Don't update the cache if the version is 'LATEST'
    if (!versionNumber.equals(LATEST_COMMIT_NAME)) {

      // Create a writable copy of the repo map

      var cm = readCache().repoMap().deepCopy();
      mp = cm.createMapIfMissing(repoName);
      // Update cache with the new local repo version
      mp.put(commitHash, versionNumber);

      readCache().repoMap(cm);
      mCacheModified = true;
      flushCache();
      log("...installed");
    }
  }

  private void ensureSysCallOkay(String output, String errorTag, String context) {
    var out2 = "\n" + output;
    if (out2.contains("\n" + errorTag)) {
      badState("System call failed for:", context, INDENT, output);
    }
  }

  /**
   * Construct a SystemCall instance. If supplied dir is null, uses repoDir()
   */
  private SystemCall newSysCall(File dir) {
    var sc = new SystemCall();
    sc.setVerbose(verbose());
    if (Files.empty(dir))
      dir = repoDir();
    sc.directory(dir);
    return sc;
  }

  private void cloneRepo() {
    var sc = newSysCall(workDir());
    sc.arg(programPath("git"), "clone", "https://github.com/" + mRepoName + ".git");
    // Git clone sends output to system error for some stupid reason
    ensureSysCallOkay(sc.systemErr(), "fatal:", "cloning repo");
  }

  // ------------------------------------------------------------------
  // Cache
  // ------------------------------------------------------------------

  private GetRepoCache.Builder readCache() {
    if (mCache == null) {
      mCache = Files.parseAbstractDataOpt(GetRepoCache.DEFAULT_INSTANCE, cacheFile()).toBuilder();
      final int EXPECTED_VERSION = 1;
      if (mCache.version() < EXPECTED_VERSION) {
        mCache = GetRepoCache.newBuilder();
      } else if (mCache.version() > EXPECTED_VERSION)
        badState("Cache version is unsupported:", INDENT, mCache);
    }
    return mCache;
  }

  private File cacheFile() {
    return new File(Files.homeDirectory(), ".getrepo_cache.json");
  }

  private void flushCache() {
    if (mCacheModified) {
      files().writePretty(cacheFile(), mCache);
      mCacheModified = false;
    }
  }

  private GetRepoCache.Builder mCache;
  private boolean mCacheModified;

  // ------------------------------------------------------------------

  private void checkoutDesiredCommit() {

    // If no hash was given, use latest
    if (nullOrEmpty(mCommitHash)) {
      commitMap();
      mCommitHash = mLatestHash;
    }

    var commitHash = mCommitHash;

    // Ensure that a commit with the requested hash exists
    var message = getCommitMessage(commitHash);
    log("Checking out commit", commitHash + ", message:", quote(message));
    log("Installing as version", mVersionNumber);

    var sc = newSysCall(null);
    sc.arg(programPath("git"), "checkout", commitHash);
    sc.call();
    ensureSysCallOkay(sc.systemErr(), "error:", "checking out commit " + commitHash);
  }

  private void modifyPom() {
    File pomFile = null;
    pomFile = new File(repoDir(), "pom.xml");
    Files.assertExists(pomFile, "can't find pom.xml file");
    var doc = parseXML(pomFile);

    Node proj = getNode(doc, "project");
    var nodeGroupId = getNode(proj, "groupId");
    var nodeArtifactId = getNode(proj, "artifactId");
    var version = getNode(proj, "version");

    log("found group:", nodeGroupId, "artifact:", nodeArtifactId, "version:", version);

    version.setTextContent(mVersionNumber);

    var modifiedPom = toXMLString(doc);
    files().writeString(pomFile, modifiedPom);
  }

  private void installRepoToLocalRepository() {
    // Perform a "mvn install"
    log("performing 'mvn install'");
    var sc = newSysCall(null);
    sc.arg(programPath("mvn"), "install");
    sc.call();
    log("...done mvn install");
    ensureSysCallOkay(sc.systemErr(), "error:", "performing 'mvn install'");
  }

  // ------------------------------------------------------------------
  // Work directory
  // ------------------------------------------------------------------

  private File workDir() {
    if (mWorkDir == null) {
      var f = new File("_SKIP_gitRepoOperWork");
      files().deleteDirectory(f, "_SKIP_");
      mWorkDir = files().mkdirs(f);
    }
    return mWorkDir;
  }

  private void discardWorkDirectory() {
    if (!Files.empty(mWorkDir)) {
      files().deleteDirectory(mWorkDir, "_SKIP_");
      mWorkDir = null;
    }
  }

  private File mWorkDir;

  // ------------------------------------------------------------------
  // Locating executable files (when running within Eclipse, it has
  // a lot of trouble finding certain executables; I guess the PATH
  // is missing some stuff).
  // ------------------------------------------------------------------

  /**
   * Attempt to locate a program file. If eclipse is false, just returns the
   * name.
   */
  private File programPath(String name) {
    File f = null;
    if (config().eclipse()) {
      f = mExeMap.get(name);
      if (f == null) {
        f = findProgramPath(name);
        mExeMap.put(name, f);
      }
    } else {
      f = new File(name);
    }
    //pr("programPath for:",name,"eclipse:",config().eclipse(),"is:",Files.infoMap(f));
    return f;
  }

  private static File findProgramPath(String progName) {
    //todo("!consider moving this to java-core?");
    String[] dirs = { "/usr/local/bin" };
    File progPath = null;
    for (var d : dirs) {
      var c = new File(d, progName);
      if (c.exists()) {
        progPath = c;
        break;
      }
    }
    if (progPath == null)
      throw FileException.withMessage("Cannot locate program:", progName);
    return progPath;
  }

  private Map<String, File> mExeMap = hashMap();

  // ------------------------------------------------------------------
  // Parsing commit hashes and messages
  // ------------------------------------------------------------------

  private Map<String, String> commitMap() {
    if (mCommitMap == null) {
      var sc = newSysCall(null);
      sc.arg(programPath("git"), "log", "--oneline");
      var out = sc.systemOut().trim();
      mCommitMap = hashMap();
      for (var logEntry : split(out, '\n')) {
        var commitHash = "";
        var commitMessage = "";
        var sep = logEntry.indexOf(' ');
        if (sep < 0) {
          badArg("no commit message found:", logEntry);
          commitHash = logEntry;
        } else {
          commitHash = logEntry.substring(0, sep);
          commitMessage = logEntry.substring(sep + 1);
        }
        if (mLatestHash == null)
          mLatestHash = commitHash;
        mCommitMap.put(commitHash, commitMessage);
      }
      checkState(!mCommitMap.isEmpty(), "Repository has no commits");
    }
    return mCommitMap;
  }

  private String getCommitMessage(String hash) {
    var message = commitMap().get(hash);
    if (message == null)
      badArg("No commit found with hash:", hash);
    return message;
  }

  private Map<String, String> mCommitMap;
  private String mLatestHash;

  /**
   * Determining the repo subdirectory within the work directory
   */
  private File repoDir() {
    if (mRepoDir == null) {
      // Determine the name of the repo
      var dw = new DirWalk(workDir()).withRecurse(false).includeDirectories();
      for (var f : dw.files()) {
        if (f.isDirectory()) {
          checkState(mRepoDir == null, "too many directories found in directory listing:", dw.files());
          mRepoDir = f;
        }
      }
      Files.assertNonEmpty(mRepoDir, "repo dir");
    }
    return mRepoDir;
  }

  private File mRepoDir;

  // ------------------------------------------------------------------
  // Parsing, encoding XML
  // ------------------------------------------------------------------

  private Document parseXML(File path) {
    try {
      var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path);
      return doc;
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private String toXMLString(Node parent) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(parent), new StreamResult(writer));
      var output = writer.getBuffer().toString();
      return output;
    } catch (Throwable t) {
      throw asRuntimeException(t);
    }
  }

  private Node getNode(Node parent, String name) {
    Node child = null;
    var ch = parent.getChildNodes();
    for (int i = 0; i < ch.getLength(); i++) {
      var c = ch.item(i);
      if (c.getNodeName().equals(name)) {
        checkState(child == null, "duplicate nodes named:", name, "for:", parent);
        child = c;
      }
    }
    checkState(child != null, "cannot find node named:", name, "for:", parent);
    return child;
  }

  private static final String LATEST_COMMIT_NAME = "LATEST";

  private String mRepoName;
  private String mCommitHash;
  private String mVersionNumber;
}
