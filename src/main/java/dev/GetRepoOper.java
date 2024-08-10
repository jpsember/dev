package dev;

import static js.base.Tools.*;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import dev.gen.GetRepoConfig;
import js.app.AppOper;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.FileException;
import js.file.Files;

public class GetRepoOper extends AppOper {

  private static final boolean NO_RECREATE = true && alert("NOT recreating clone dir");

  @Override
  public String userCommand() {
    return "getrepo";
  }

  @Override
  public String getHelpDescription() {
    return "no help is available for operation (yet)";
  }

  @Override
  public GetRepoConfig defaultArgs() {
    pr(GetRepoConfig.DEFAULT_INSTANCE);
    return GetRepoConfig.DEFAULT_INSTANCE;
  }

  @Override
  public GetRepoConfig config() {
    if (mConfig == null) {
      mConfig = (GetRepoConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    determineRepoAndVersion();
  }

  private void ensureSysCallOkay(String output, String errorTag, String context) {
    var out2 = "\n" + output;
    if (out2.contains("\n" + errorTag)) {
      badState("System call failed for:", context, INDENT, output);
    }
  }

  private void determineRepoAndVersion() {
    var name = config().name();
    checkNonEmpty(name, "No repo name given");

    // We want to be able to execute something like
    //
    //   git clone https://github.com/jpsember/java-core.git
    //
    var sc = new SystemCall();

    sc.setVerbose(verbose());
    sc.directory(workDir());
    sc.arg(programPath("git"), "clone", "https://github.com/" + name + ".git");

    if (!NO_RECREATE) {
      // Git clone sends output to system error for some stupid reason
      ensureSysCallOkay(sc.systemErr(), "fatal:", "cloning repo");
    }

    // Determine the name of the repo
    var dw = new DirWalk(workDir()).withRecurse(false).includeDirectories();
    File repoDir = null;
    for (var f : dw.files()) {
      if (f.isDirectory()) {
        checkState(repoDir == null, "too many directories found in directory listing:", dw.files());
        repoDir = f;
      }
    }

    Files.assertNonEmpty(repoDir, "repo dir");

    {
      sc = new SystemCall();
      sc.directory(repoDir);
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
        mCommitMap.put(commitHash, commitMessage);
      }
    }

    var hash = config().hash().toLowerCase();

    checkState(!nullOrEmpty(hash), "for now, please supply an explicit commit hash");
    var versionNumber = config().version();
    checkState(!nullOrEmpty(versionNumber), "please specify a version to assign to this commit");

    {
      var message = mCommitMap.get(hash);
      if (message == null)
        badArg("No commit found with hash:", hash);

      sc = new SystemCall();
      sc.directory(repoDir);
      sc.arg(programPath("git"), "checkout", hash);
      sc.call();
      ensureSysCallOkay(sc.systemErr(), "error:", "checking out commit " + hash);
    }

    File pomFile = null;
    try {
      pomFile = new File(repoDir, "pom.xml");
      Files.assertExists(pomFile, "can't find pom.xml file");

      var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile);

      Node proj = getNode(doc, "project");

      var nodeGroupId = getNode(proj, "groupId");
      var nodeArtifactId = getNode(proj, "artifactId");
      var version = getNode(proj, "version");

      log("found group:", nodeGroupId, "artifact:", nodeArtifactId, "version:", version);

      version.setTextContent(versionNumber);

      var modifiedPom = xmlToString(doc);
      files().writeString(pomFile, modifiedPom);

    } catch (Throwable t) {
      throw asRuntimeException(t);
    }

    // Perform a "mvn install"

    // halt("do a mvn install");
    sc = new SystemCall();
    sc.directory(repoDir);
    sc.arg(programPath("mvn"), "install");
    sc.call();
    ensureSysCallOkay(sc.systemErr(), "error:", "performing 'mvn install'");
  }

  public static File findProgramPath(String progName) {
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

  private String xmlToString(Node parent) {
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
    pr("getting node named:", name, "from parent:", parent);
    Node child = null;
    var ch = parent.getChildNodes();
    pr("attrib:", ch);
    for (int i = 0; i < ch.getLength(); i++) {
      var c = ch.item(i);
      pr("child:", i, "is:", c);
      if (c.getNodeName().equals(name)) {
        checkState(child == null, "duplicate nodes named:", name, "for:", parent);
        child = c;
      }
    }
    checkState(child != null, "cannot find node named:", name, "for:", parent);
    return child;
  }

  private File workDir() {
    if (mWorkDir == null) {
      var f = new File("_SKIP_gitRepoOperWork");
      if (!NO_RECREATE)
        files().deleteDirectory(f, "_SKIP_");
      mWorkDir = files().mkdirs(f);
      todo("clean up work directory when done");
    }
    return mWorkDir;
  }

  private File programPath(String name) {
    var f = mExeMap.get(name);
    if (f == null) {
      f = findProgramPath(name);
      mExeMap.put(name, f);
    }
    return f;
  }

  private Map<String, File> mExeMap = hashMap();
  private File mWorkDir;
  private GetRepoConfig mConfig;
  private Map<String, String> mCommitMap;
}
