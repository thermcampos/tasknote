package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface contains methods to access the user table in the database. */
@Repository
public class UserRepository {

  private static final String COLUMNS = """
      id, email, password, admin, created_at, inactivated_at, name, email_confirmed_at, \
      email_uuid, reset_password_expiration, reset_token, lang, theme, last_password_change, \
      last_login""";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public UserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save a User (create or update).
   *
   * @param user The User instance to be created or updated.
   * @return The created or updated User instance.
   */
  public User save(User user) {
    return user.getId() == null
        ? create(user)
        : update(user);
  }

  private User create(User user) {
    String sql = """
      INSERT INTO tasknote.users (
        email, password, admin, created_at, inactivated_at, name, email_confirmed_at, email_uuid,
        reset_password_expiration, reset_token, lang, theme, last_password_change, last_login)
      VALUES (
        :email, :password, :admin, :createdAt, :inactivatedAt, :name, :emailConfirmedAt, :emailUuid,
        :resetPasswordExpiration, :resetToken, :lang, :theme, :lastPasswordChange, :lastLogin)
      RETURNING\s""" + COLUMNS;

    MapSqlParameterSource params = userParams(user);

    return jdbcTemplate.queryForObject(sql, params, new UserRowMapper());
  }

  private User update(User user) {
    String sql = """
      UPDATE tasknote.users
      SET email = :email,
          password = :password,
          admin = :admin,
          created_at = :createdAt,
          inactivated_at = :inactivatedAt,
          name = :name,
          email_confirmed_at = :emailConfirmedAt,
          email_uuid = :emailUuid,
          reset_password_expiration = :resetPasswordExpiration,
          reset_token = :resetToken,
          lang = :lang,
          theme = :theme,
          last_password_change = :lastPasswordChange,
          last_login = :lastLogin
      WHERE id = :id
      RETURNING\s""" + COLUMNS;

    MapSqlParameterSource params = userParams(user)
        .addValue("id", user.getId());

    return jdbcTemplate.queryForObject(sql, params, new UserRowMapper());
  }

  /**
   * Delete a User from the database.
   *
   * @param user The User instance to be deleted (needs the ID).
   */
  public void delete(User user) {
    String sql = "DELETE FROM tasknote.users WHERE id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", user.getId());

    jdbcTemplate.update(sql, params);
  }

  /**
   * Find a User by ID.
   *
   * @param id The User ID to search by.
   * @return Optional of User. Can be empty.
   */
  public Optional<User> findById(Long id) {
    String sql = "SELECT " + COLUMNS + " FROM tasknote.users WHERE id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", id);

    return findOne(sql, params);
  }

  /**
   * Find a User by email.
   *
   * @param email The email to search by.
   * @return Optional of User. Can be empty.
   */
  public Optional<User> findByEmail(String email) {
    String sql = "SELECT " + COLUMNS + " FROM tasknote.users WHERE email = :email";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("email", email);

    return findOne(sql, params);
  }

  /**
   * Find a User by email UUID (email confirmation).
   *
   * @param uuid The email UUID to search by.
   * @return Optional of User. Can be empty.
   */
  public Optional<User> findByEmailUuid(UUID uuid) {
    String sql = "SELECT " + COLUMNS + " FROM tasknote.users WHERE email_uuid = :uuid";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("uuid", uuid);

    return findOne(sql, params);
  }

  /**
   * Find a User by reset token (password reset).
   *
   * @param token The reset token to search by.
   * @return Optional of User. Can be empty.
   */
  public Optional<User> findByResetToken(String token) {
    String sql = "SELECT " + COLUMNS + " FROM tasknote.users WHERE reset_token = :token";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("token", token);

    return findOne(sql, params);
  }

  private Optional<User> findOne(String sql, MapSqlParameterSource params) {
    List<User> users = jdbcTemplate.query(sql, params, new UserRowMapper());
    return users.stream().findFirst();
  }

  private MapSqlParameterSource userParams(User user) {
    return new MapSqlParameterSource()
        .addValue("email", user.getEmail())
        .addValue("password", user.getPassword())
        .addValue("admin", user.getAdmin())
        .addValue("createdAt", user.getCreatedAt())
        .addValue("inactivatedAt", user.getInactivatedAt())
        .addValue("name", user.getName())
        .addValue("emailConfirmedAt", user.getEmailConfirmedAt())
        .addValue("emailUuid", user.getEmailUuid())
        .addValue("resetPasswordExpiration", user.getResetPasswordExpiration())
        .addValue("resetToken", user.getResetToken())
        .addValue("lang", user.getLang())
        .addValue("theme", user.getTheme())
        .addValue("lastPasswordChange", user.getLastPasswordChange())
        .addValue("lastLogin", user.getLastLogin());
  }

  class UserRowMapper implements org.springframework.jdbc.core.RowMapper<User> {
    @Override
    public User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      User user = new User();
      user.setId(rs.getLong("id"));
      user.setEmail(rs.getString("email"));
      user.setPassword(rs.getString("password"));
      user.setAdmin(rs.getBoolean("admin"));
      user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
      user.setInactivatedAt(rs.getObject("inactivated_at", LocalDateTime.class));
      user.setName(rs.getString("name"));
      user.setEmailConfirmedAt(rs.getObject("email_confirmed_at", LocalDateTime.class));
      user.setEmailUuid(rs.getObject("email_uuid", UUID.class));
      user.setResetPasswordExpiration(
          rs.getObject("reset_password_expiration", LocalDateTime.class));
      user.setResetToken(rs.getString("reset_token"));
      user.setLang(rs.getString("lang"));
      user.setTheme(rs.getString("theme"));
      user.setLastPasswordChange(rs.getObject("last_password_change", LocalDateTime.class));
      user.setLastLogin(rs.getObject("last_login", LocalDateTime.class));
      return user;
    }
  }
}
