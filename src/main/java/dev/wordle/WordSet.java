package dev.wordle;

import js.base.BaseObject;
import js.file.FileException;
import js.file.Files;
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

  public static DictionaryEntry bigDictionary() {
    return dictEntry("big");
  }

  public static DictionaryEntry smallDictionary() {
    return dictEntry("small");
  }

  public static DictionaryEntry selectDict(String name) {
    DictionaryEntry ent = dictEntry(name);
    if (ent == null)
      return null;
    sDefaultDictionary = ent;
    sWordBytes = ent.dictionary.wordBytes();
    return ent;
  }

  public static DictionaryEntry defaultDictEntry() {
    if (sDefaultDictionary == null)
      selectDict("big");
    return sDefaultDictionary;
  }


  private static DictionaryEntry dictEntry(String name) {
    DictionaryEntry entry = sEntryMap.get(name);
    if (entry == null) {
      Dictionary d = readDictionary(name);
      if (d != null) {
        entry = new DictionaryEntry();
        entry.name = name;
        entry.dictionary = d;
        {
          int k = d.words().size();
          int[] wordIds = new int[k];
          for (int i = 0; i < k; i++)
            wordIds[i] = i * WORD_LENGTH;
          entry.wordSet = withWordIds(wordIds);
        }
        sEntryMap.put(name, entry);
      }
    }
    return entry;
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
  private static DictionaryEntry sDefaultDictionary;
  private static byte[] sWordBytes;
  

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

  public Word getWordWithId(int id) {
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

  public int getWordId(int wordNumber) {
    return mWordIds[wordNumber];
  }

  // Ids of words in this dictionary.  An id is its index within the master dictionary
  //
  private int[] mWordIds;


}
