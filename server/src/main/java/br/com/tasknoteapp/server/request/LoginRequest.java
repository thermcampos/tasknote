package br.com.tasknoteapp.server.request;

/** This class represents a login request with user email and password. */
public record LoginRequest(String email, String password, String passwordAgain, String lang) {

  @Override
  public String toString() {
    return "LoginRequest{"
        + "email=' + email + '"
        + ",password='[REDACTED]'"
        + ",passwordAgain='[REDACTED]'"
        + ",lang=' + lang + '"
        + "}";
  }
}
