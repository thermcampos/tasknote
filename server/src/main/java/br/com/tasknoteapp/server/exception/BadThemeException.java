package br.com.tasknoteapp.server.exception;

/** This class represents a Bad Theme exception. */
public class BadThemeException extends BaseBadRequestException {

  public BadThemeException() {
    super("theme", "Invalid theme");
  }
}
