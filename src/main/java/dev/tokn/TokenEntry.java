package dev.tokn;

public class TokenEntry {

  final String name;
  final RegParse reg_ex;
  final int id;

  public TokenEntry(String name, RegParse regEx, int id) {
    this.name = name;
    this.reg_ex = regEx;
    this.id = id;
  }

}
