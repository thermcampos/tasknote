package br.com.tasknoteapp.server.request;

import jakarta.validation.constraints.NotBlank;

/** This class represents a delete account request carrying the current user password. */
public record DeleteAccountRequest(@NotBlank String password) {

  @Override
  public String toString() {
    return "DeleteAccountRequest{password='[REDACTED]'}";
  }
}
