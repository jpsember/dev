package dev.wordle;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;

import static dev.wordle.WordleUtils.*;

import java.util.List;
import java.util.Map;

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

  private static String sName = "big";

  public static Dictionary defaultDictionary() {
    defaultSet();
    return sDictionary;
  }

  private static WordSet sLargeSet;

  public static WordSet largeSet() {
    if (sLargeSet == null) {
      if (sName.equals("big"))
        sLargeSet = sDefaultWordSet;
      else {
        sLargeSet = readSet("big");
      }
    }
    return sLargeSet;
  }

  public static Dictionary dict(String name) {
    Dictionary d = sDicts.get(name);
    if (d == null) {
      d = readDictionary2(name);
      sDicts.put(name, d);
    }
    return d;
  }

  private static Map<String, Dictionary> sDicts = hashMap();

  private static Dictionary readDictionary2(String name) {
    Dictionary dict = null;
    try {
      dict = WordSet.readDictionary(name);
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
    } catch (Throwable e) {
      throw asRuntimeException(e);
    }
    return dict;
  }

  private static WordSet readSet(String name) {
    Dictionary dict = dict(name);
    sLastDictRead = dict;
    int k = dict.words().size();
    int[] wordIds = new int[k];
    for (int i = 0; i < k; i++)
      wordIds[i] = i * WORD_LENGTH;
    return withWordIds(wordIds);
  }

  private static Dictionary sLastDictRead;

  public static WordSet defaultSet() {
    if (sDefaultWordSet == null) {
      WordSet ws = readSet(sName);
      sWordBytes = sLastDictRead.wordBytes();
      sDefaultWordSet = ws;
      sDictionary = sLastDictRead;
    }
    return sDefaultWordSet;
  }

  public static WordSet withWordIds(int[] wordIds) {
    WordSet d = new WordSet();
    if (VERIFY) {
      for (int id : wordIds) {
        verify(id);
      }
    }
    d.mWordIds = wordIds;
    return d;
  }

  public int size() {
    return mWordIds.length;
  }

  public int wordId(int index) {
    return mWordIds[index];
  }

  public Word getWordWithId(int id) {
    verify(id);
    return new Word(sWordBytes, id);
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

  public List<Word> getWords(int[] ids) {
    List<Word> words = arrayList();
    for (int id : ids)
      words.add(getWordWithId(id));
    return words;
  }

  public static List<String> getWordStrings(List<Word> words) {
    List<String> poss = arrayList();
    for (Word w : words)
      poss.add(w.toString());
    return poss;
  }

  // Ids of words in this dictionary.  An id is its index within the master dictionary
  //
  private int[] mWordIds;

  private static Dictionary readDictionary(String name) {
    String listName = Files.setExtension(name, Files.EXT_JSON);
    JSMap m = JSMap.fromResource(WordSet.class, listName);

    if (false && alert("trimming plurals")) {
      JSList newWordList = list();
      for (String word : m.getList("words").asStrings()) {
        boolean skip = false;
        if (word.charAt(4) == 'S') {
          if ("AEIOU".indexOf(word.charAt(4)) < 0) {
            skip = true;
          }
        }
        if (!skip)
          newWordList.add(word);
      }
      m.put("words", newWordList);
    }
    if (false && alert("sorting")) {
      Dictionary d = Files.parseAbstractDataOpt(Dictionary.DEFAULT_INSTANCE, m);
      List<String> s = arrayList();
      s.addAll(d.words());
      s.sort(null);
      if (!s.equals(d.words())) {
        Dictionary.Builder db = d.toBuilder();
        db.words(s);
        Files.S.writeString(Files.getDesktopFile(listName), db.toJson().toString());
        halt("words sorted are different");
      }
    }

    return Files.parseAbstractDataOpt(Dictionary.DEFAULT_INSTANCE, m);
  }

  private static Dictionary sDictionary;
  private static WordSet sDefaultWordSet;
  private static byte[] sWordBytes;

  public int getWordId(int wordNumber) {
    return mWordIds[wordNumber];
  }

}
