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

import dev.gen.RemoteEntityInfo;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.base.SystemCall;
import js.file.Files;

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
    if (mEntityNameExpr == null) {
      displayEntity();
    } else
      switch (mEntityNameExpr) {
      default:
        setEntity();
        break;
      case "list":
        pr(EntityManager.sharedInstance().entityMap());
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
      for (RemoteEntityInfo ent : mgr.entityMap().entityMap().values()) {
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
      setError("no entity found for:", quote(expr), INDENT, mgr.entityMap());
      return;
    }
    mgr.setActive(foundEnt.tag());
    createSSHScript(foundEnt);
    displayEntity();
  }

  private void createSSHScript(RemoteEntityInfo ent) {
    StringBuilder sb = new StringBuilder();
    sb.append("#!/usr/bin/env bash\n");
    sb.append("echo \"Connecting to: ");
    sb.append(ent.tag());
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
