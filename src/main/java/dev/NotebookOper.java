package dev;

import static js.base.Tools.*;

import dev.gen.NotebookConfig;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;

import java.io.File;

public class NotebookOper extends AppOper {

  @Override
  public String userCommand() {
    return "notebook";
  }

  @Override
  public String shortHelp() {
    return "convert Jupyter notebook <=> python script";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("x", "(not finished)");
    b.pr(hf);
    b.br();
    b.pr("(not finished)");
  }

  @Override
  public NotebookConfig defaultArgs() {
    return NotebookConfig.DEFAULT_INSTANCE;
  }

  @Override
  @SuppressWarnings("unchecked")
  public NotebookConfig config() {
    if (mConfig == null) {
      mConfig = super.config();
    }
    return mConfig;
  }

  private static final String EXT_NOTEBOOK = "ipynb";
  private static final String EXT_PYTHON = "py";


  @Override
  public void perform() {
    var inp = Files.assertNonEmpty(config().input(), "input file");
    Files.assertExists(inp, "input file");

    var fromNotebook = isNotebook(inp);

    var out = config().output();
    if (Files.empty(out)) {
      out = Files.setExtension(inp, fromNotebook ? EXT_PYTHON : EXT_NOTEBOOK);
    }
    var toNotebook = isNotebook(out);
    if (fromNotebook == toNotebook)
      throw setError("extensions must be different");

    if (toNotebook) {
      doToNotebook(inp, out);
    } else {
      doToScript(inp, out);
    }
  }


  /**
   * Determine if file is a notebook (.ipynyb) vs python (.py)
   */
  private boolean isNotebook(File f) {
    var ext = Files.getExtension(f);
    if (ext.equals(EXT_NOTEBOOK)) {
      return true;
    } else if (ext.equals(EXT_PYTHON)) {
      return false;
    } else {
      throw setError("unsupported extension:", ext);
    }
  }


  private void doToScript(File inp, File out) {

    var m = JSMap.from(inp);

    var cells = m.getList("cells");

    var sb = new StringBuilder();

    if (false) {
      sb.append("#!/usr/bin/env python\n");
      sb.append("# coding: utf-8");
    }

    for (int i = 0; i < cells.size(); i++) {
      JSMap c = cells.getMap(i);

      //log("Cell:", INDENT, c);
      switch (c.get("cell_type")) {
        case "code": {
          sb.append("\n\n# In[ ]:\n\n\n");
          var src = c.getList("source");
          for (var x : src.asStringList()) {
            sb.append(x);
          }
        }
        break;
      }
    }

    var content = sb.toString();
    files().writeString(out, content);
  }

  private void doToNotebook(File inp, File out) {

    var notebookTemplate = Files.readString(this.getClass(), "notebook_template.json");
    JSMap outputJson = new JSMap(notebookTemplate);
    mCells = list();
    outputJson.put("cells", mCells);

    mCellTemplate = new JSMap(Files.readString(getClass(), "cell_template.json"));


    var pythonSource = Files.readString(inp);
    mPythonSourceLines = pythonSource.lines().toArray(String[]::new);


    // #   In[  42 ]:
    var chunkMarkerPat = RegExp.pattern("^#\\s*In\\[\\s*\\d*\\s*\\]:\\s*$");

    var chunkStart = 0;

    int i = INIT_INDEX;
    for (var s : mPythonSourceLines) {
      i++;

      if (RegExp.patternMatchesString(chunkMarkerPat, s)) {
        flushChunk(chunkStart, i);
        chunkStart = i + 1;
      }
    }

    flushChunk(chunkStart, mPythonSourceLines.length);

    var content = config().prettyPrint() ? outputJson.prettyPrint() : outputJson.toString();
    files().writeString(out, content);
  }

  private void flushChunk(int x0, int x1) {
    if (x0 == x1) return;

    // Eliminate blank lines bordering this chunk
    while (x0 < x1 && mPythonSourceLines[x0].isEmpty())
      x0++;
    while (x1 - 1 > x0 && mPythonSourceLines[x1 - 1].isEmpty())
      x1--;

    var cell = mCellTemplate.deepCopy();
    var srcLines = list();
    cell.put("source", srcLines);
    for (var j = x0; j < x1; j++)
      srcLines.add(mPythonSourceLines[j] + "\n");
    mCells.add(cell);

  }

  private NotebookConfig mConfig;
  private String[] mPythonSourceLines;
  private JSMap mCellTemplate;
  private JSList mCells;
}
