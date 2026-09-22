package br.com.tasknoteapp.server.exception;

/** This class represents client errors during user sign up. */
public class SignUpException extends BaseBadRequestException {
  
  public SignUpException(String message) {
    super("signIn", message);
  }
}
