/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
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

import dev.gen.RemoteEntityInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;

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
      } else if (arg.equals("OVERRIDE")) {
        mOverrideFlag = true;
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

  private boolean mOverrideFlag;

  @Override
  public void perform() {
    if (verbose()) {
      manager().setVerbose();
      Ngrok.sharedInstance().setVerbose();
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
      pr(manager().currentEntities());
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
    RemoteEntityInfo ent = manager().optionalActiveEntity();
    if (ent == null)
      pr("{}");
    else
      pr(ent);
  }

  private void setEntity(String id) {
    RemoteEntityInfo foundEnt = manager().optionalEntryFor(id);
    if (foundEnt == null) {
      setError("no entity found for:", quote(id), "; use 'list' to available ones");
    }
    RemoteEntityInfo updatedEnt = updateEntity(foundEnt);
    updatedEnt = manager().updateEnt(updatedEnt);
    manager().setActive(updatedEnt.id());
    createSSHScript(updatedEnt.id());
    displayEntity();
  }

  private void addEntity(String id) {
    if (!mOverrideFlag) {
      setError("For simplicity, at present, this command should only be run from Jeff's machine.",CR, //
          "Include the argument 'OVERRIDE' if you really want to do this."
          );
    }
   
    RemoteEntityInfo foundEnt = manager().optionalEntryFor(id);
    if (foundEnt != null) {
      setError("entity already exists:", INDENT, foundEnt);
    }
    manager().create(RemoteEntityInfo.newBuilder().id(id));
  }

  private RemoteEntityInfo updateEntity(RemoteEntityInfo entity) {
    RemoteEntityInfo.Builder b = entity.build().toBuilder();
    RemoteEntityInfo tunnel = Ngrok.sharedInstance().tunnelInfo(entity.id());
    if (tunnel == null) {
      pr("*** no ngrok tunnel found for entity:", entity.id());
    } else {
      b.url(tunnel.url());
      b.port(tunnel.port());
    }
    return b.build();
  }

  private void createSSHScript(String tag) {
    StringBuilder sb = new StringBuilder();
    RemoteEntityInfo ent = manager().entryFor(tag);
    sb.append("#!/usr/bin/env bash\n");
    sb.append("echo \"Connecting to: ");
    sb.append(tag);
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
  private String mIdArg;
  private String mOperName;
}
