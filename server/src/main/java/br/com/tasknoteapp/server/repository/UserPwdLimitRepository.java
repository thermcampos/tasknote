package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.UserPwdLimit;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a user password limit repository, for database access. */
@Repository
public class UserPwdLimitRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public UserPwdLimitRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save a User Password Limit (create or update).
   *
   * @param userPwdLimit The UserPwdLimit instance to be created or updated.
   * @return The created or updated UserPwdLimit instance.
   */
  public UserPwdLimit save(UserPwdLimit userPwdLimit) {
    return userPwdLimit.getId() == null
        ? create(userPwdLimit)
        : update(userPwdLimit);
  }

  private UserPwdLimit create(UserPwdLimit userPwdLimit) {
    String sql =
        """
        INSERT INTO tasknote.user_pwd_limits (user_id, when_happened)
        VALUES (:userId, :whenHappened)
        RETURNING id, user_id, when_happened
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userPwdLimit.getUserId())
        .addValue("whenHappened", userPwdLimit.getWhenHappened());

    return jdbcTemplate.queryForObject(sql, params, new UserPwdLimitRowMapper());
  }

  private UserPwdLimit update(UserPwdLimit userPwdLimit) {
    String sql =
        """
        UPDATE tasknote.user_pwd_limits
        SET when_happened = :whenHappened
        WHERE id = :id
        RETURNING id, user_id, when_happened
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("whenHappened", userPwdLimit.getWhenHappened())
        .addValue("id", userPwdLimit.getId());

    return jdbcTemplate.queryForObject(sql, params, new UserPwdLimitRowMapper());
  }

  /**
   * Find the last 3 User Password Limit records given a User ID, most recent first.
   *
   * @param userId The User ID to search by.
   * @return List of up to 3 UserPwdLimit found.
   */
  public List<UserPwdLimit> findTop3ByUser_idOrderByWhenHappenedDesc(Long userId) {
    String sql =
        """
        SELECT id, user_id, when_happened
        FROM tasknote.user_pwd_limits
        WHERE user_id = :userId
        ORDER BY when_happened DESC
        LIMIT 3
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.query(sql, params, new UserPwdLimitRowMapper());
  }

  /**
   * Delete all User Password Limit records given a User ID.
   *
   * @param userId The User ID to delete for.
   */
  public void deleteAllForUser(Long userId) {
    String sql =
        """
        DELETE FROM tasknote.user_pwd_limits
        WHERE user_id = :userId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    jdbcTemplate.update(sql, params);
  }

  class UserPwdLimitRowMapper implements org.springframework.jdbc.core.RowMapper<UserPwdLimit> {
    @Override
    public UserPwdLimit mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new UserPwdLimit(
          rs.getLong("id"),
          rs.getObject("when_happened", LocalDateTime.class),
          rs.getLong("user_id")
      );
    }
  }
}

