package com.delta.dms.community.exception;

public class UnauthorizedException extends RuntimeException {
  private static final long serialVersionUID = -8494469376746439135L;

  public UnauthorizedException(String message) {
    super(message);
  }
}
