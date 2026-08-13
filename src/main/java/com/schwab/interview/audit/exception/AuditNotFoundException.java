package com.schwab.interview.audit.exception;

public class AuditNotFoundException extends RuntimeException {
  public AuditNotFoundException(String message) {
    super(message);
  }
}
