package com.yerbie.core.exception;

public class SerializationException extends RuntimeException {
  public SerializationException() {
    super("Could not serialize job data.");
  }
}
