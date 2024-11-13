package com.delta.dms.community.exception;

public class UpdateFailedException extends RuntimeException {
  private static final long serialVersionUID = -8494469376746439135L;

  public UpdateFailedException(String message) {
    super(message);
  }
}
