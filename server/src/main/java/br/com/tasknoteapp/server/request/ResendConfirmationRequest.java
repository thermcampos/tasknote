package br.com.tasknoteapp.server.request;

/** This class represents a login request with user email and password. */
public record ResendConfirmationRequest(String email) {}
