package dev;

import static js.base.Tools.*;

import java.io.File;

import dev.gen.NewOperConfig;
import js.app.AppOper;
import js.data.DataUtil;
import js.file.Files;
import js.json.JSMap;
import js.parsing.MacroParser;

public class NewOperOper extends AppOper {

  @Override
  public String userCommand() {
    return "newoper";
  }

  @Override
  public String shortHelp() {
    return "creates a new operation for the dev program";
  }

  @Override
  public NewOperConfig defaultArgs() {
    return NewOperConfig.DEFAULT_INSTANCE;
  }

  @Override
  public NewOperConfig config() {
    if (mConfig == null) {
      mConfig = (NewOperConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    File srcDir = new File("src/main/java/dev");
    Files.assertDirectoryExists(srcDir, "current directory should be the dev project repo");

    {
      String n = config().name();
      checkNonEmpty(n, "missing argument: name");
      n = DataUtil.convertUnderscoresToCamelCase(n);
      checkArgument(n.endsWith("Oper"), "name should end with 'Oper':", n);
      mClassName = n;
    }
    {
      String n = config().configName();
      n = DataUtil.convertUnderscoresToCamelCase(n);
      if (n.isEmpty()) {
        n = chomp(mClassName, "Oper") + "Config";
      }
      mConfigClassName = n;
    }

    File mainClass = Files.assertExists(new File(srcDir, "Main.java"), "main class file");
    String txt = Files.readString(mainClass);
    String newLine = "registerOper(new " + mClassName + "());";
    checkState(!txt.contains(newLine), "oper already exists");
    File datFile = new File(
        "dat_files/dev/gen/" + DataUtil.convertCamelCaseToUnderscores(mConfigClassName) + ".dat");
    checkState(!datFile.exists(), "dat_file already exists:", datFile);

    int i = txt.indexOf("    // --- insertion point for new operations (used by NewOperOper)");
    checkState(i >= 0, "can't find insertion point");
    txt = txt.substring(0, i) + "    " + newLine + "\n" + txt.substring(i);
    files().writeString(mainClass, txt);

    {
      File operFile = new File(srcDir, mClassName + ".java");
      String template = frag("oper_java_template.txt");
      String result = MacroParser.parse(template, macroMap());
      files().writeString(operFile, result);
    }
    {
      String template = frag("oper_config_template.txt");
      String result = MacroParser.parse(template, macroMap());
      files().writeString(datFile, result);
    }

  }

  private String frag(String resourceName) {
    return Files.readString(getClass(), "newoper/" + resourceName);
  }

  private JSMap macroMap() {
    if (mMacroMap == null) {
      JSMap m = map();
      m.put("config_class_name", mConfigClassName);
      m.put("class_name", mClassName);
      m.put("help_description", config().helpDescription());
      String userCmd = config().userCommand();
      if (userCmd.isEmpty()) {
        userCmd = chomp(mClassName, "Oper").toLowerCase();
      }
      m.put("user_command", userCmd);
      mMacroMap = m;
    }
    return mMacroMap;
  }

  private NewOperConfig mConfig;
  private String mClassName;
  private String mConfigClassName;
  private JSMap mMacroMap;

}
