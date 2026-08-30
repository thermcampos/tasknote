package br.com.tasknoteapp.server.exception;

/** This class represents a Max login limit exception. */
public class MaxLoginLimitAttemptException extends BaseBadRequestException {

  public MaxLoginLimitAttemptException() {
    super("login", "Max login attempt limit reached. Please wait 3 minutes");
  }
}
