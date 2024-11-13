package com.delta.dms.community.exception;

public class AuthenticationException extends RuntimeException {
  private static final long serialVersionUID = -8494469376746439135L;

  public AuthenticationException(String message) {
    super(message);
  }
}
