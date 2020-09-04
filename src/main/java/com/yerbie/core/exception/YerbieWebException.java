package com.yerbie.core.exception;

import javax.ws.rs.WebApplicationException;

public class YerbieWebException extends WebApplicationException {
  public YerbieWebException(String message, int responseCode) {
    super(message, responseCode);
  }
}
