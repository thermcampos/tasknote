package br.com.tasknoteapp.server.request;

/** This class represents a delete account request carrying the current user password. */
public record DeleteAccountRequest(String password) {

  @Override
  public String toString() {
    return "DeleteAccountRequest{password='[REDACTED]'}";
  }
}
