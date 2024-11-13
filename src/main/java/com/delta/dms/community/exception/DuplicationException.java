package com.delta.dms.community.exception;

public class DuplicationException extends RuntimeException {
  private static final long serialVersionUID = -8494469376746439135L;

  public DuplicationException(String message) {
    super(message);
  }
}
