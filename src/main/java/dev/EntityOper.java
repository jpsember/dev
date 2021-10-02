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
import java.util.regex.Matcher;

import dev.gen.RemoteEntityInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;

public class EntityOper extends AppOper {

  @Override
  public String userCommand() {
    return "entity";
  }

  @Override
  public String getHelpDescription() {
    return "selects active remote entity";
  }

  @Override
  protected List<Object> getAdditionalArgs() {
    return arrayList("[<tag or name> | list]");
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    if (args.hasNextArg()) {
      mEntityNameExpr = args.nextArg();
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    if (verbose())
      EntityManager.sharedInstance().setVerbose();
    if (mEntityNameExpr == null) {
      displayEntity();
    } else
      switch (mEntityNameExpr) {
      default:
        setEntity();
        break;
      case "list":
        pr(EntityManager.sharedInstance().entities());
        break;
      }
  }

  private void displayEntity() {
    RemoteEntityInfo ent = EntityManager.sharedInstance().optionalActiveEntity();
    if (ent == RemoteEntityInfo.DEFAULT_INSTANCE)
      pr("<none>");
    else
      pr(ent.tag(), INDENT, ent);
  }

  private void setEntity() {
    EntityManager mgr = EntityManager.sharedInstance();
    RemoteEntityInfo foundEnt = null;
    String expr = mEntityNameExpr;
    String exprLower = expr.toLowerCase();
    outer: for (int pass = 0; pass < 2; pass++) {
      for (RemoteEntityInfo ent : mgr.entities().entityMap().values()) {
        if (pass == 0) {
          if (ent.tag().equals(expr) || ent.longName().equalsIgnoreCase(expr)) {
            foundEnt = ent;
            break outer;
          }
        } else {
          if (ent.longName().toLowerCase().startsWith(exprLower)) {
            foundEnt = ent;
            break outer;
          }
        }
      }
    }

    if (foundEnt == null) {
      setError("no entity found for:", quote(expr), INDENT, mgr.entities());
      return;
    }

    RemoteEntityInfo updatedEnt = updateEntity(foundEnt);
    mgr.updateEnt(updatedEnt);
    mgr.setActive(updatedEnt.tag());
    createSSHScript(updatedEnt.tag());
    displayEntity();
  }

  private RemoteEntityInfo updateEntity(RemoteEntityInfo entity) {
    RemoteEntityInfo.Builder b = entity.build().toBuilder();

    JSMap m = Ngrok.sharedInstance().tunnels();

    JSMap activeTunnel = null;

    // TODO: we're digging into the JSMap returned by Ngrok here, when ideally this would be internal to the Ngrok class.
    // TODO: we're performing a validation over *all* the tunnels, when we're actually only interested in modifying the current one.
    JSList tunnels = m.getList("tunnels");
    for (JSMap tunMap : tunnels.asMaps()) {
      String metadata = tunMap.get("metadata");

      if (alert("temporarily acting as if metadata exists for one of the tunnels")) {
        if (metadata.isEmpty() && tunMap.get("public_url").contains("2.tcp.ngrok.io")) {
          metadata = "rpi";
          tunMap.put("metadata", metadata);
        }
      }
      if (metadata.isEmpty()) {
        pr("*** ngrok tunnel has no metadata:", INDENT, tunMap);
      } else {
        RemoteEntityInfo info = EntityManager.sharedInstance().optionalEntryFor(metadata);
        if (info == RemoteEntityInfo.DEFAULT_INSTANCE) {
          pr("*** ngrok tunnel metadata doesn't correspond to any remote entities:", INDENT, tunMap);
        } else {
          if (info.tag().equals(entity.tag())) {
            activeTunnel = tunMap;
          }
        }
      }
    }
    if (activeTunnel == null) {
      pr("*** no ngrok tunnel found for entity:", entity.tag());
    } else {
      String publicUrl = activeTunnel.get("public_url");
      chompPrefix(publicUrl, "tcp://");
      Matcher matcher = RegExp.matcher("tcp:\\/\\/(.+):(\\d+)", publicUrl);
      if (!matcher.matches()) {
        pr("*** failed to parse public_url:", publicUrl);
      } else {
        b.url(matcher.group(1));
        b.port(Integer.parseInt(matcher.group(2)));
      }
    }
    return b.build();
  }

  private void createSSHScript(String tag) {
    StringBuilder sb = new StringBuilder();
    RemoteEntityInfo ent = EntityManager.sharedInstance().entryFor(tag);
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
    if (!dryRun()) {
      SystemCall sc = new SystemCall();
      sc.setVerbose(verbose());
      sc.arg("chmod", "u+x", f);
      sc.assertSuccess();
    }
  }

  private String mEntityNameExpr;
}
