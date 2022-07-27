package dev.wordle;

import static dev.wordle.WordleUtils.*;
import static js.base.Tools.*;

import java.util.Map;

import dev.gen.wordle.Dictionary;
import js.file.FileException;
import js.file.Files;
import js.json.JSMap;

public final class DictionaryEntry {

  public static DictionaryEntry big() {
    return get("big");
  }

  public static DictionaryEntry small() {
    return get("small");
  }

  /**
   * Select dictionary by name; return null if not found
   */
  public static DictionaryEntry select(String name) {
    DictionaryEntry ent = get(name);
    if (ent == null)
      return null;
    sActive = ent;
    sWordBytes = ent.dictionary().wordBytes();
    return ent;
  }

  public static DictionaryEntry active() {
    return sActive;
  }

  /**
   * Get active dictionary's list of word bytes
   */
  public static byte[] wordBytes() {
    return sWordBytes;
  }

  public static DictionaryEntry create(String name, Dictionary d) {
    DictionaryEntry entry = new DictionaryEntry(d);
    sEntryMap.put(name, entry);
    return entry;
  }

  private static DictionaryEntry get(String name) {
    return sEntryMap.get(name);
  }

  private static Dictionary readDictionary(String name) {
    Dictionary dict = null;
    try {
      String listName = Files.setExtension(name, Files.EXT_JSON);
      JSMap m = null;
      try {
        m = JSMap.fromResource(WordSet.class, listName);
      } catch (FileException e) {
        pr("...no such dictionary!");
        return null;
      }
      Dictionary.Builder db = Files.parseAbstractDataOpt(Dictionary.DEFAULT_INSTANCE, m).toBuilder();
      byte[] b = new byte[db.words().size() * WORD_LENGTH];
      int c = 0;
      for (String s : db.words()) {
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

  private static Map<String, DictionaryEntry> sEntryMap = hashMap();
  private static DictionaryEntry sActive;
  private static byte[] sWordBytes;

  static {
    create("big", readDictionary("big"));
    create("small", readDictionary("small"));
    select("big");
  }

  private DictionaryEntry(Dictionary d) {
    mDictionary = d;
    int k = d.words().size();
    int[] wordIds = new int[k];
    for (int i = 0; i < k; i++)
      wordIds[i] = i * WORD_LENGTH;
    mWordSet = WordSet.withWordIds(wordIds);
  }

  public WordSet wordSet() {
    return mWordSet;
  }

  public Dictionary dictionary() {
    return mDictionary;
  }

  private final Dictionary mDictionary;
  private final WordSet mWordSet;

}