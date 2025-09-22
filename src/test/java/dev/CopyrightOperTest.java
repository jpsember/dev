package dev;

import js.file.Files;
import org.junit.Test;

public class CopyrightOperTest extends DevTestBase {

  public CopyrightOperTest() {
    setOper("copyright");
  }

  @Test
  public void java() {
    proc("sd_java");
  }

  @Test
  public void javaRemove() {
    addArg("remove_message");
    java();
  }

  @Test
  public void rust() {
    provideArg("file_extensions", "rs");
    provideArg("copyright_text_file", generatedFile("header.txt"));
    proc("sd_rust");
  }

  @Test
  public void rustRemove() {
    addArg("remove_message");
    rust();
  }

  private void proc(String subdirName) {
    // class {
    //    File source_dir;
    //    *string file_extensions;
    //    File copyright_text_file;
    //    string header_reg_ex;
    //    bool remove_message;
    //}
    var srcDir = Files.join(testDataDir(), subdirName);
    files().copyDirectory(srcDir, generatedDir());
    provideArg("source_dir", generatedDir());
    runApp();
    assertGenerated();
  }

}
