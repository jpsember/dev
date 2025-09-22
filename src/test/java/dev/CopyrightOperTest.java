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
