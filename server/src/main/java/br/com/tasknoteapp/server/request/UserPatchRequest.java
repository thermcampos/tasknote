package br.com.tasknoteapp.server.request;

/** This record represents a user patch payload. */
public record UserPatchRequest(
    String name,
    String email,
    String password,
    String passwordAgain,
    String lang,
    String currentPassword) {}

