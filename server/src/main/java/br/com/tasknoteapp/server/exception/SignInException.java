package br.com.tasknoteapp.server.exception;

/** This class represents client errors during user sign in. */
public class SignInException extends BaseBadRequestException {
  
  public SignInException(String message) {
    super("signIn", message);
  }
}
