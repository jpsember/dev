package dev.wordle;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSMap;

import static dev.wordle.WordleUtils.*;

import java.io.File;
import java.util.List;

import dev.gen.wordle.Dictionary;

import static js.base.Tools.*;

/**
 * A set of words taken from a Dictionary
 */
public final class WordSet extends BaseObject {

  public static void selectDictionary(String name) {
    sName = name;
    sDictionary = null;
    sDefaultWordSet = null;
    sWordBytes = null;
    defaultDictionary();
  }

  private static String sName = "mit";

  public static Dictionary defaultDictionary() {
    defaultSet();
    return sDictionary;
  }

  public static WordSet defaultSet() {
    if (sDefaultWordSet == null) {
      Dictionary dict;
      try {
        dict = WordSet.readDictionary(sName);
        Dictionary.Builder db = dict.toBuilder();
        byte[] b = new byte[dict.words().size() * WORD_LENGTH];
        int c = 0;
        for (String s : dict.words()) {
          byte[] sourceBytes = s.getBytes("UTF-8");
          System.arraycopy(sourceBytes, 0, b, c, WORD_LENGTH);
          c += WORD_LENGTH;
        }
        db.wordBytes(b);
        dict = db.build();
        sDictionary = dict;
      } catch (Throwable e) {
        throw asRuntimeException(e);
      }
      sWordBytes = dict.wordBytes();
      int k = dict.words().size();
      int[] wordIds = new int[k];
      for (int i = 0; i < k; i++)
        wordIds[i] = i;
      sDefaultWordSet = withWordIds(wordIds);
    }
    return sDefaultWordSet;
  }

  public static WordSet withWordIds(int[] wordIds) {
    WordSet d = new WordSet();
    d.mWordIds = wordIds;
    return d;
  }

  public int size() {
    return mWordIds.length;
  }

  public int wordId(int index) {
    return mWordIds[index];
  }

  public void getWord(Word target, int index) {
    target.set(sWordBytes, wordId(index));
  }

  public Word getWord(int index) {
    return new Word(sWordBytes, wordId(index));
  }

  public List<Word> getWords() {
    List<Word> words = arrayList();
    for (int id : mWordIds)
      words.add(new Word(sWordBytes, id));
    return words;
  }

  public List<String> getWordStrings(int[] ids) {
    List<Word> words = getWords(ids);
    List<String> strs = arrayList();
    for (Word w : words)
      strs.add(w.toString());
    return strs;
  }

  public List<Word> getWords(int[] w) {
    List<Word> words = arrayList();
    for (int wi : w)
      words.add(getWord(wi));
    return words;
  }

  // Ids of words in this dictionary.  An id is its index within the master dictionary
  //
  private int[] mWordIds;

  public static Dictionary readDictionary(String name) {
    String listName = Files.setExtension(name, Files.EXT_JSON);
    JSMap m = JSMap.fromResource(WordSet.class, listName);
    return Files.parseAbstractDataOpt(Dictionary.DEFAULT_INSTANCE, m);
  }

  @Deprecated
  public static void generateResource(String listName, String s) {
    File dir = Files.getDesktopDirectory();
    File target = new File(dir, Files.setExtension(listName, Files.EXT_JSON));
    if (false && target.exists()) {
      pr("...already exists:", target);
      return;
    }
    Dictionary.Builder dict = Dictionary.newBuilder();
    dict.name(listName);
    List<String> words = arrayList();
    int line = INIT_INDEX;
    for (String ln : split(s, '\n')) {
      line++;
      if (ln.isEmpty())
        continue;
      if (ln.length() != WORD_LENGTH)
        badArg("line number:", line, quote(ln));
      words.add(ln);
    }
    dict.words(words);
    String encoded = dict.toJson().toString();
    Files.S.writeString(target, encoded);
    pr("...wrote:", target);
  }

  private static Dictionary sDictionary;
  private static WordSet sDefaultWordSet;
  private static byte[] sWordBytes;

}
