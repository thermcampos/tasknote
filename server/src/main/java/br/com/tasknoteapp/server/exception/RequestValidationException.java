package br.com.tasknoteapp.server.exception;

/** This class represents client errors during user sign in. */
public class RequestValidationException extends BaseBadRequestException {
  
  public RequestValidationException(String key, String message) {
    super(key, message);
  }
}
