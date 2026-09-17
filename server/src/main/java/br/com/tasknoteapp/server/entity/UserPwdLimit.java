package br.com.tasknoteapp.server.entity;

import java.time.LocalDateTime;

/** This class represents a User Password Limit in the database. */
public class UserPwdLimit {

  private Long id;
  private LocalDateTime whenHappened;
  private Long userId;

  public UserPwdLimit() {}

  public UserPwdLimit(Long id, LocalDateTime whenHappened, Long userId) {
    this.id = id;
    this.whenHappened = whenHappened;
    this.userId = userId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public LocalDateTime getWhenHappened() {
    return whenHappened;
  }

  public void setWhenHappened(LocalDateTime whenHappened) {
    this.whenHappened = whenHappened;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPwdLimit that = (UserPwdLimit) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "UserPwdLimitEntity{"
        + "id="
        + id
        + ", whenHappened="
        + whenHappened
        + ", userId="
        + userId
        + '}';
  }
}
