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
import java.util.List;
import java.util.Set;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;
import js.webtools.EntityManager;
import js.webtools.Ngrok;
import js.webtools.gen.RemoteEntityInfo;

public class EntityOper extends AppOper {

  @Override
  public String userCommand() {
    return "entity";
  }

  @Override
  public String getHelpDescription() {
    return "manage remote entities";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[[select] <id> | add <id> | list]");
  }

  private static Set<String> sOperNames = hashSetWith("display", "select", "list", "add");

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    while (args.hasNextArg()) {
      String arg = args.nextArg();
      if (sOperNames.contains(arg)) {
        if (mOperName != null && !arg.equals(mOperName))
          throw badArg("cannot process multiple operations:", mOperName, arg);
        mOperName = arg;
      } else {
        if (mIdArg != null)
          throw badArg("extraneous argument:", arg);
        mIdArg = arg;
        if (mOperName == null)
          mOperName = "select";
      }
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    mNgrok = new Ngrok();
    if (verbose()) {
      manager().setVerbose();
      mNgrok.setVerbose();
    }
    if (mOperName == null)
      mOperName = "display";

    switch (mOperName) {
    default:
      throw badState("unsupported operation:", mOperName);
    case "display":
      displayEntity();
      break;
    case "select":
      setEntity(consumeIdArg());
      break;
    case "list":
      pr(manager().registry());
      break;
    case "add": {
      String id = consumeIdArg();

      addEntity(id);
      setEntity(id);
    }
      break;
    }
    if (mIdArg != null)
      checkState(mIdArgUsed, "extraneous argument:", mIdArg);
  }

  private String consumeIdArg() {
    checkState(mIdArg != null, "expected an extra argument");
    checkState(!mIdArgUsed, "arg already used");
    mIdArgUsed = true;
    return mIdArg;
  }

  private boolean mIdArgUsed;

  private void displayEntity() {
    String activeEntityId = manager().activeEntityId();
    if (nullOrEmpty(activeEntityId))
      pr("{}");
    else
      pr(manager().entity(activeEntityId));
  }

  private void setEntity(String id) {
    RemoteEntityInfo ent = manager().entity(id);

    // RemoteEntityInfo foundEnt = manager().optionalEntryFor(id);
    if (ent == null) {
      setError("no entity found for:", quote(id), "; use 'list' to available ones");
    }

    RemoteEntityInfo modified = mNgrok.addNgrokInfo(ent);
    if (modified == null)
      setError("no ngrok info found for:", quote(id));
    manager().setActive(id);
    createSSHScript(modified);
    displayEntity();
  }

  private void addEntity(String id) {
    RemoteEntityInfo ent = manager().entity(id);
    if (ent != null)
      setError("entity already exists:", INDENT, ent);
    manager().create(RemoteEntityInfo.newBuilder().id(id));
  }

  private void createSSHScript(RemoteEntityInfo ent) {
    StringBuilder sb = new StringBuilder();
    sb.append("#!/usr/bin/env bash\n");
    sb.append("echo \"Connecting to: ");
    sb.append(ent.id());
    sb.append("\"\n");
    sb.append("ssh ");
    sb.append(ent.user());
    sb.append("@");
    sb.append(ent.url());
    sb.append(" -p ");
    sb.append(ent.port());
    sb.append(" -oStrictHostKeyChecking=no");
    sb.append(" $@");
    sb.append('\n');
    File f = new File(Files.homeDirectory(), "bin/sshe");
    files().writeString(f, sb.toString());
    files().chmod(f, "u+x");
  }

  private EntityManager manager() {
    if (mEntityManager == null) {
      mEntityManager = new EntityManager().withFiles(files());
    }
    return mEntityManager;
  }

  private EntityManager mEntityManager;
  private Ngrok mNgrok;
  private String mIdArg;
  private String mOperName;
}
