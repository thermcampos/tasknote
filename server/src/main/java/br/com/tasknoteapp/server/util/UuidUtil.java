package br.com.tasknoteapp.server.util;

import java.util.UUID;

/** This class provides method to handle UUIDs. */
public class UuidUtil {

  /**
   * Generates a cryptographically random UUID for use as an email confirmation token.
   *
   * @param email The user email (unused; kept for API compatibility).
   * @return A random UUID.
   */
  public UUID generateEmailUuid(String email) {
    return UUID.randomUUID();
  }
}
