package br.com.tasknoteapp.server.request;

/** This record represents a user patch payload. */
public record UserPatchRequest(
    String name,
    String email,
    String password,
    String passwordAgain,
    String lang,
    String theme,
    String currentPassword) {

  @Override
  public String toString() {
    return "UserPatchRequest{"
        + "name=' + name + '"
        + ",email=' + email + '"
        + ",password='[REDACTED]'"
        + ",passwordAgain='[REDACTED]'"
        + ",lang=' + lang + '"
        + ",theme=' + theme + '"
        + ",currentPassword='[REDACTED]'"
        + "}";
  }
}
