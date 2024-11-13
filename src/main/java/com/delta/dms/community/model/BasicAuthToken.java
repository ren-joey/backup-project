package com.delta.dms.community.model;

public class BasicAuthToken {

  private static ThreadLocal<String> token = ThreadLocal.withInitial(() -> "");

  private BasicAuthToken() {}

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
