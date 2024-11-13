package com.delta.dms.community.exception;

public class UpdateConflictException extends RuntimeException {
  private static final long serialVersionUID = -8494469376746439135L;

  public UpdateConflictException(String message) {
    super(message);
  }
}
