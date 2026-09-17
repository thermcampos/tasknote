package br.com.tasknoteapp.server.response;

import br.com.tasknoteapp.server.entity.User;
import java.time.LocalDateTime;

/** This record represents a User Response object. */
public record UserResponseWithToken(
    Long userId,
    String name,
    String email,
    Boolean admin,
    LocalDateTime createdAt,
    LocalDateTime inactivatedAt,
    LocalDateTime lastLogin,
    String gravatarImageUrl,
    String token,
    String lang) {

  /**
   * Create a {@link UserResponseWithToken} instance from a {@link User}.
   *
   * @param user The user entity instance with user info to be used as source.
   * @param token The token created upon registration or login.
   * @return UserResponse instance.
   */
  public static UserResponseWithToken fromEntity(
      User user, String token, String gravatarUrl) {
    return new UserResponseWithToken(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getAdmin(),
        user.getCreatedAt(),
        user.getInactivatedAt(),
        user.getLastLogin(),
        gravatarUrl,
        token,
        user.getLang());
  }
}
