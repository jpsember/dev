package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
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
import js.file.Files;

public class GetRepoOper extends AppOper {

  @Override
  public String userCommand() {
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
    hf.addItem("[ purge ]","purge our cached info about GitHub");
    b.pr(hf);
    b.br();
    b.pr("If no hash and version number are given, uses the most recent commit with version = '"
        + LATEST_COMMIT_NAME + "'.");
  }

  private static final String LATEST_COMMIT_NAME = "1000";

  private static final String CKEY_HASH_TO_VERSION_MAP = "vmap";
  private static final String CKEY_LAST_RECENT_HASH = "recent";

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

  // ------------------------------------------------------------------
  // Cache
  // ------------------------------------------------------------------

  private GetRepoCache.Builder readCache() {
    if (mCache == null) {
      if (config().purge()) {
        mCache = GetRepoCache.newBuilder();
        mCacheModified = true;
      } else
        mCache = Files.parseAbstractDataOpt(GetRepoCache.DEFAULT_INSTANCE, cacheFile()).toBuilder();
      final int EXPECTED_VERSION = 2;
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
    for (var c : childNodes(parent)) {
      if (c.getNodeName().equals(name)) {
        checkState(child == null, "duplicate nodes named:", name, "for:", parent);
        child = c;
      }
    }
    checkState(child != null, "cannot find node named:", name, "for:", parent);
    return child;
  }

  private List<Node> childNodes(Node parent) {
    List<Node> children = arrayList();
    var ch = parent.getChildNodes();
    for (int i = 0; i < ch.getLength(); i++) {
      children.add(ch.item(i));
    }
    return children;
  }

  @Override
  public void perform() {

    var projectPomFile = new File("pom.xml");
    Files.assertExists(projectPomFile, "can't find project pom.xml file");

    var projectPom = parseXML(projectPomFile);
    var depsNode = getNode(getNode(projectPom, "project"), "dependencies");

    List<Node> depList = arrayList();
    for (var dc : childNodes(depsNode)) {
      if (dc.getNodeName().equals("dependency")) {
        depList.add(dc);
      }
    }

    var entries = config().entries();

    if (entries.isEmpty())
      log("...no entries found in config file");
    Set<String> processed = hashSet();
    for (var ent : entries) {
      var repoName = ent.repoName();
      if (!processed.add(repoName))
        badArg("duplicate repo_name:", repoName);
      new Handler().processEntry(ent, depList);
    }

    if (mPomModified) {
      var newContent = toXMLString(projectPom);
      files().writeString(projectPomFile, newContent);
    }
    flushCache();
  }

  private boolean mPomModified;

  private class Handler {

    public void processEntry(GetRepoEntry entry, List<Node> dependencies) {
      log("processing entry:", INDENT, entry);
      var repoName = entry.repoName();
      if (nullOrEmpty(repoName))
        setError("Illegal repo name:", INDENT, entry);
      repoName = chomp(repoName, ".git");

      var hash = entry.commitHash().toLowerCase();
      var versionNumber = entry.version();

      if (config().latest()) {
        hash = "";
        versionNumber = "";
      }

      if (nullOrEmpty(hash) && nullOrEmpty(versionNumber)) {
        versionNumber = LATEST_COMMIT_NAME;
      } else {
        checkState(!nullOrEmpty(hash), "supply an explicit commit hash");
        checkState(!nullOrEmpty(versionNumber) && !versionNumber.equals(LATEST_COMMIT_NAME),
            "please specify a version to assign to commit", hash);
      }
      readRepo(repoName, hash, versionNumber);

      // Modify the dependency that matches this repo

      var x = entry.repoName();
      var parts = split(x, '/');
      checkArgument(parts.size() == 2, "unexpected parts for:", x);
      var currentArtifactId = parts.get(1);

      {
        Node depNode = null;
        for (var n : dependencies) {
          var artifactId = getNode(n, "artifactId");
          if (!artifactId.getTextContent().equals(currentArtifactId))
            continue;

          // If there is more than a single dependency with this artifactId, 
          // we can include a group_id key in the arguments to disambiguate
          // which one we want.

          var groupText = getNode(n, "groupId").getTextContent();
          if (nonEmpty(entry.groupId()) && !entry.groupId().equals(groupText))
            continue;

          checkState(depNode == null, "duplicate artifact name found");
          depNode = n;
        }
        if (depNode == null) {
          badState("can't find dependency in pom that matches repo:", entry.repoName());
        }

        var version = getNode(depNode, "version");
        var oldVersionNum = version.getTextContent();
        if (!versionNumber.equals(oldVersionNum)) {
          log("updating version number in pom from", oldVersionNum, "to", versionNumber);
          version.setTextContent(versionNumber);
          mPomModified = true;
        }
      }

      discardWorkDirectory();
    }

    private void readRepo(String repoName, String commitHash, String versionNumber) {
      log("reading repo", repoName, "commit", commitHash, "to install locally as version", versionNumber);

      mRepoName = repoName;
      var latestCommitFlag = nullOrEmpty(commitHash);
      mVersionNumber = versionNumber;

      var mp2 = readCache().repoMap().optJSMapOrEmpty(repoName);

      var versionMap = mp2.createMapIfMissing(CKEY_HASH_TO_VERSION_MAP);
      String existingVersion = versionMap.opt(commitHash, "");
      if (!nullOrEmpty(existingVersion)) {
        checkState(existingVersion.equals(versionNumber), "Repo", repoName, "commit", commitHash,
            "exists in repo but with a different version number:", existingVersion);
        log("...already installed");
        return;
      }

      cloneRepo();

      if (latestCommitFlag) {
        commitHash = latestHash();
        var installed = mp2.opt(CKEY_LAST_RECENT_HASH, "");
        if (installed.equals(commitHash)) {
          log("...most recent installed commit matches latest commit");
          return;
        }
      }

      checkoutDesiredCommit(commitHash);

      modifyPom();
      installRepoToLocalRepository();

      log("...done install to local, discard work dir:", mRepoDir);

      {
        // Create a writable copy of the repo map JSMap

        var mutableRepoMap = readCache().repoMap().deepCopy();
        var repoInfoMap = mutableRepoMap.createMapIfMissing(repoName);
        var hashToVersionMap = repoInfoMap.createMapIfMissing(CKEY_HASH_TO_VERSION_MAP);

        if (latestCommitFlag) {
          repoInfoMap.put(CKEY_LAST_RECENT_HASH, latestHash());
        } else {
          // Update cache with the new local repo version
          hashToVersionMap.put(commitHash, versionNumber);
        }

        readCache().repoMap(mutableRepoMap);
        mCacheModified = true;
        flushCache();
        log("...installed");
      }

      discardWorkDirectory();
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
      sc.arg(Files.programPath("git"), "clone", "https://github.com/" + mRepoName + ".git");
      // Git clone sends output to system error for some stupid reason
      ensureSysCallOkay(sc.systemErr(), "fatal:", "cloning repo");
    }

    // ------------------------------------------------------------------

    private void checkoutDesiredCommit(String commitHash) {

      // Ensure that a commit with the requested hash exists
      var message = getCommitMessage(commitHash);
      log("Checking out commit", commitHash + ", message:", quote(message));
      log("Installing as version", mVersionNumber);

      var sc = newSysCall(null);
      sc.arg(Files.programPath("git"), "checkout", commitHash);
      sc.call();
      ensureSysCallOkay(sc.systemErr(), "error:", "checking out commit " + commitHash);
    }

    private void modifyPom() {
      File depPomFile = null;
      depPomFile = new File(repoDir(), "pom.xml");
      Files.assertExists(depPomFile, "can't find pom.xml file");
      var doc = parseXML(depPomFile);

      Node proj = getNode(doc, "project");
      var nodeGroupId = getNode(proj, "groupId");
      var nodeArtifactId = getNode(proj, "artifactId");
      var version = getNode(proj, "version");

      log("found group:", nodeGroupId.getNodeName(), "artifact:", nodeArtifactId.getNodeName(), "version:",
          version.getNodeName());

      version.setTextContent(mVersionNumber);

      var modifiedPom = toXMLString(doc);
      files().writeString(depPomFile, modifiedPom);
    }

    private void installRepoToLocalRepository() {
      log("performing 'mvn install'");
      var sc = newSysCall(null);
      sc.arg(Files.programPath("mvn"), "install");
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
    // Parsing commit hashes and messages
    // ------------------------------------------------------------------

    private Map<String, String> commitMap() {
      if (mCommitMap == null) {
        boolean first = true;
        var sc = newSysCall(null);
        sc.arg(Files.programPath("git"), "log", "--oneline");
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
          if (first) {
            mHashOfLatestCommit = commitHash;
            first = false;
          }
          mCommitMap.put(commitHash, commitMessage);
        }
        checkState(!mCommitMap.isEmpty(), "Repository has no commits");
      }
      return mCommitMap;
    }

    private String getCommitMessage(String hash) {
      var message = commitMap().get(hash);
      if (message == null)
        badArg("Repo", mRepoName, "has no commit hash", hash);
      return message;
    }

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

    private String latestHash() {
      commitMap();
      return mHashOfLatestCommit;
    }

    private String mRepoName;
    private String mVersionNumber;
    private Map<String, String> mCommitMap;
    private String mHashOfLatestCommit;
    private File mRepoDir;

  }

}
