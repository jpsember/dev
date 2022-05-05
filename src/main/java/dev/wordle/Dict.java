package dev.wordle;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSMap;

import static dev.wordle.WordleUtils.*;

import java.io.File;
import java.util.List;

import dev.gen.wordle.Dictionary;

import static js.base.Tools.*;

public final class Dict extends BaseObject {

  public static Dict standard() {
    if (sDefaultDict == null) {
     int k = dictionary().words().size();
      int[] wordIds = new int[k];
      for (int i = 0; i < k; i++)
        wordIds[i] = i;
      sDefaultDict = withWordIds(wordIds);
    }
    return sDefaultDict;
  }

  public static Dict withWordIds(int[] wordIds) {
    Dict d = new Dict();
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
    byte[] wl = wordList();
    target.set(wl, wordId(index));
  }

  public Word getWord(int index) {
    return new Word(wordList(), wordId(index));
  }

  public List<Word> getWords() {
    List<Word> words = arrayList();
    for (int id : mWordIds)
      words.add(new Word(wordList(), id));
    return words;
  }

  public List<Word> getWords(int[] w) {
    List<Word> words = arrayList();
    for (int wi : w)
      words.add(getWord(wi));
    return words;
  }

  public List<String> wordStrings(int[] wordIndices) {
    Word work = Word.buildEmpty();
    List<String> result = arrayList();
    for (int x : wordIndices) {
      getWord(work, x);
      result.add(work.toString());
    }
    return result;
  }

  // Ids of words in this dictionary.  An id is its index within the master dictionary
  //
  private int[] mWordIds;

  private static Dict sDefaultDict;

  public static Dictionary readDictionary(String name) {
    String listName = Files.setExtension(name, Files.EXT_JSON);
    JSMap m = JSMap.fromResource(Dict.class, listName);
    return Files.parseAbstractDataOpt(Dictionary.DEFAULT_INSTANCE, m);
  }

  public static void generateResource(String listName, String s) {
    File dir = Files.getDesktopDirectory();//new File("src/main/resources/dev/wordle");
//    Files.assertDirectoryExists(dir, "resources");
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
      if (ln.isEmpty()) continue;
      if (ln.length() != WORD_LENGTH)
        badArg("line number:", line, quote(ln));
      words.add(ln);
    }
    dict.words(words);
    String encoded = dict.toJson().toString();
    Files.S.writeString(target, encoded);
    pr("...wrote:", target);
  }

}
