package dev;

import js.file.Files;
import org.junit.Test;

import java.io.File;

public class CollectErrorsOperTest extends DevTestBase {

  public CollectErrorsOperTest() {
    setOper("collect-errs");
  }

  @Test
  public void java() {
    proc("sd_java");
  }

  @Test
  public void rust() {
    proc("sd_rust");
  }

  private void proc(String subdirName) {
    var srcDir = new File("unit_test/copyright_oper_test_data");
    srcDir = Files.join(srcDir, subdirName);
    files().copyDirectory(srcDir, generatedDir());
    provideArg("input", generatedDir());
    provideArg("output", Files.join(generatedDir(), "result.txt"));
    runApp();
    assertGenerated();
  }

}
