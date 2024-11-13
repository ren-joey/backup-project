package com.delta.dms.community.model;

public class Jwt {

  private static ThreadLocal<String> token = ThreadLocal.withInitial(() -> "");

  private Jwt() {}

  public static String get() {
    return token.get();
  }

  public static void set(String jwt) {
    token.set(jwt);
  }

  public static void unset() {
    token.remove();
  }
}
