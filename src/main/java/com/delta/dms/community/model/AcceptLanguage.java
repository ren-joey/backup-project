package com.delta.dms.community.model;

import java.util.Locale;
import com.delta.dms.community.enums.DbLanguage;

public class AcceptLanguage {

  private static ThreadLocal<String> language =
      ThreadLocal.withInitial(() -> Locale.TRADITIONAL_CHINESE.toLanguageTag().toLowerCase());
  private static ThreadLocal<String> languageForDb =
      ThreadLocal.withInitial(DbLanguage.ZHTW::toString);

  public static String getLanguageForDb() {
    return languageForDb.get();
  }

  public static void setLanguageForDb(String lang) {
    languageForDb.set(lang);
  }

  private AcceptLanguage() {}

  public static String get() {
    return language.get();
  }

  public static void set(String lang) {
    lang = lang.toLowerCase();
    language.set(lang);
    languageForDb.set(langConvert(lang));
  }

  public static void unset() {
    language.remove();
    languageForDb.remove();
  }

  public static String convertToLocaleString(String lang) {
    switch (lang) {
      case "zh":
      case "zh_tw":
      case "zh-tw":
        return Locale.TRADITIONAL_CHINESE.toLanguageTag().toLowerCase();
      case "en":
      case "en_us":
      case "en-us":
        return Locale.US.toLanguageTag().toLowerCase();
      case "zh_cn":
      case "zh-cn":
        return Locale.SIMPLIFIED_CHINESE.toLanguageTag().toLowerCase();
      default:
        return lang;
    }
  }

  private static String langConvert(String lang) {
    switch (lang) {
      case "zh":
      case "zh_tw":
      case "zh-tw":
        return DbLanguage.ZHTW.toString();
      case "en":
      case "en_us":
      case "en-us":
        return DbLanguage.ENUS.toString();
      case "zh_cn":
      case "zh-cn":
        return DbLanguage.ZHCN.toString();
      default:
        return DbLanguage.ZHTW.toString();
    }
  }
}
