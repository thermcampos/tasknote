package br.com.tasknoteapp.server.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/** This class represents a login request with user email and password. */
@NotNull
public class LoginRequest {
  @Email @NotNull private String email;

  @NotNull private String password;

  private final String passwordAgain;

  private final String lang;

  /**
   * Constructs a LoginRequest with the specified email and password.
   *
   * @param email the user email
   * @param password the user password
   * @param passwordAgain the user password again
   * @param lang the user language
   */
  public LoginRequest(String email, String password, String passwordAgain, String lang) {
    this.email = email;
    this.password = password;
    this.passwordAgain = passwordAgain;
    this.lang = lang;
  }

  public String email() {
    return email.trim().toLowerCase();
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String password() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String passwordAgain() {
    return passwordAgain;
  }

  public String lang() {
    return lang;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoginRequest that = (LoginRequest) o;
    return email().equals(that.email()) && password().equals(that.password());
  }

  @Override
  public int hashCode() {
    int result = email().hashCode();
    result = 31 * result + password().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "LoginRequest{"
        + "email='"
        + email
        + '\''
        + ", password='[REDACTED]'"
        + ", passwordAgain='[REDACTED]'"
        + ", lang='"
        + lang
        + '\''
        + '}';
  }
}
