package br.com.tasknoteapp.server.request;

/** This record represents the confirmation of the password reset. */
public record PasswordResetRequest(String token, String password, String passwordAgain) {}
