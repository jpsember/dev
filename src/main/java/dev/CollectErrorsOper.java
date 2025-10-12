package dev;

import static js.base.Tools.*;

import dev.gen.CollectErrorsConfig;
import dev.gen.ErrInfo;
import js.app.AppOper;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.file.DirWalk;
import js.file.Files;
import js.parsing.DFA;
import js.parsing.Lexeme;
import js.parsing.Lexer;

import java.util.List;
import java.util.Map;

public class CollectErrorsOper extends AppOper {

  @Override
  public String userCommand() {
    return "collect-errs";
  }

  @Override
  public String shortHelp() {
    return "collect error codes into a table";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("***", "** no 'long' help available yet");
    b.pr(hf);
    b.br();
  }

  @Override
  public CollectErrorsConfig defaultArgs() {
    return CollectErrorsConfig.DEFAULT_INSTANCE;
  }

  @Override
  public CollectErrorsConfig config() {
    if (mConfig == null) {
      mConfig = (CollectErrorsConfig) super.config();
    }
    return mConfig;
  }

  @Override
  public void perform() {
    var inputDirectory = Files.assertDirectoryExists(config().input(), "input directory");
    var dfa = DFA.parse(Files.readString(getClass(), "collect_errors.dfa"));
    var extensions = arrayList("java", "rs", "py");
    var dirWalk = new DirWalk(inputDirectory).withRecurse(true).withExtensions(extensions);

    var b = ErrInfo.newBuilder();

    for (var sourceFile : dirWalk.files()) {
      var fileType = extensions.indexOf(Files.getExtension(sourceFile));
      checkArgument(fileType >= 0);
      var txt = Files.readString(sourceFile);
      var scanner = new Lexer(dfa);
      scanner.withSkipId(T_SPACES);
      scanner.withAcceptUnknownTokens();
      scanner.withText(txt);
      scanner.setVerbose();

      mCommentsBuffer.clear();

      // If a COMMENT is found, add to the comment buffer
      // If a CR is found, clear the comments
      // If something other than ERRCODE_xxxx, read and continue

      while (scanner.hasNext()) {

        mErrCodeToken = null;
        if (scanner.readIf(T_CR)) {
          mCommentsBuffer.clear();
        } else if (scanner.readIf(T_COMMENT)) {
          mCommentsBuffer.add(scanner.token().text());
        } else {
          switch (fileType) {
            case 0: // java
              //  public static final int ERRCODE_FOO_BAR = 8200;
              if (scanner.readIf(T_PUBLIC, T_STATIC, T_FINAL, T_INT, T_ERRCODE, T_EQUALS, T_NUMBER, T_SEMICOLON)) {
                mErrCodeToken = scanner.token(0);
                b.name(extractErrName(scanner.token(4)));
                b.id(Integer.parseInt(scanner.token(6).text()));
                b.description(compileDescription());
              }
              break;
            case 1: // rust
              // pub const ERRCODE_MULTIPLE_GEOMETRY_COLUMNS: usize = 1002;
              if (scanner.readIf(T_PUB, T_CONST, T_ERRCODE, T_COLON, T_USIZE, T_EQUALS, T_NUMBER, T_SEMICOLON)) {
                mErrCodeToken = scanner.token(0);
                b.name(extractErrName(scanner.token(2)));
                b.id(Integer.parseInt(scanner.token(6).text()));
                b.description(compileDescription());
              }
              break;
            case 2: // py
              // ERRCODE_NOT_IMPLEMENTED = 9999
              if (scanner.readIf(T_ERRCODE, T_EQUALS, T_NUMBER)) {
                mErrCodeToken = scanner.token(0);
                b.name(extractErrName(scanner.token(0)));
                b.id(Integer.parseInt(scanner.token(2).text()));
                b.description(compileDescription());
              }
              break;
          }

          if (b.id() == 0) {
            scanner.read();
          } else {
            var rec = b.build();
            if (mInfoMap.containsKey(rec.id())) {
              setError("duplicate error number:", INDENT, rec, CR, mInfoMap.get(rec.id()));
            }
            mInfoMap.put(rec.id(), rec);
            log("Storing:", scanner.token(0), INDENT, rec);
            mCommentsBuffer.clear();
            b.id(0);
          }
        }
      }
    }

    String result;
    {
      var sb = new StringBuilder();
      ErrInfo prevEnt = null;
      for (var errInfo : mInfoMap.values()) {
        if (prevEnt != null && errInfo.id() / 1000 != prevEnt.id() / 1000)
          sb.append("\n");
        prevEnt = errInfo;
        sb.append(String.format("%5d %s", errInfo.id(), chompPrefix(errInfo.name(), "ERRCODE_")));
        sb.append('\n');
        for (var line : split(errInfo.description(), '\n')) {
          sb.append(spaces(6));
          sb.append(line);
          sb.append('\n');
        }
      }
      result = sb.toString();
    }

    var outputFile = config().output();

    if (Files.empty(outputFile)) {
      System.out.println(result);
    } else {
      log("\n", insertLeftMarginChars(result.toString()));
      files().writeIfChanged(outputFile, result);
    }
  }


  private String compileDescription() {
    // Construct description of error from buffered comments
    var expr = new StringBuilder();
    for (var c : mCommentsBuffer) {
      var c2 = chompPrefix(c, "# ");
      if (c2 == c)
        c2 = chompPrefix(c, "// ");
      expr.append(c2);
    }
    var desc = expr.toString();
    if (desc.isEmpty()) {
      mErrCodeToken.failWith("no comment found");
    }
    return desc;
  }

  private String extractErrName(Lexeme lex) {
    return chompPrefix(lex.text(), "ERRCODE_");
  }

  private static String insertLeftMarginChars(String text) {
    var sb = new StringBuilder();
    for (var ls : split(text, '\n')) {
      sb.append("|  ");
      sb.append(ls);
      sb.append('\n');
    }
    return sb.toString();
  }

  private CollectErrorsConfig mConfig;
  private Map<Integer, ErrInfo> mInfoMap = treeMap();
  private Lexeme mErrCodeToken;
  private List<String> mCommentsBuffer = arrayList();

  // Token Ids generated by 'dev dfa' tool (DO NOT EDIT BELOW)
  private static final int T_SPACES = 0;
  private static final int T_CR = 1;
  private static final int T_COMMENT = 2;
  private static final int T_NUMBER = 3;
  private static final int T_EQUALS = 4;
  private static final int T_ERRCODE = 5;
  private static final int T_PUBLIC = 6;
  private static final int T_STATIC = 7;
  private static final int T_FINAL = 8;
  private static final int T_INT = 9;
  private static final int T_SEMICOLON = 10;
  private static final int T_COLON = 11;
  private static final int T_PUB = 12;
  private static final int T_CONST = 13;
  private static final int T_USIZE = 14;
  // End of token Ids generated by 'dev dfa' tool (DO NOT EDIT ABOVE)

}
