package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.TaskUrl;
import br.com.tasknoteapp.server.entity.TaskUrlPk;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a task url repository, for database access. */
@Repository
public class TaskUrlRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public TaskUrlRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save all Task URLs (insert).
   *
   * @param taskUrls List of TaskUrl instances to be created.
   * @return Number of created records.
   */
  public int saveAll(List<TaskUrl> taskUrls) {
    String sql = """
      INSERT INTO tasknote.task_url (task_id, url)
      VALUES (:taskId, :url)
    """;

    MapSqlParameterSource[] params = taskUrls.stream()
        .map(taskUrl -> new MapSqlParameterSource()
            .addValue("taskId", taskUrl.getId().getTaskId())
            .addValue("url", taskUrl.getId().getUrl()))
        .toArray(MapSqlParameterSource[]::new);

    return Arrays.stream(jdbcTemplate.batchUpdate(sql, params)).sum();
  }

  /**
   * Delete all Task URLs given a Task ID.
   *
   * @param taskId The Task ID to delete for.
   * @return Number of deleted records.
   */
  public int deleteAllById_taskId(Long taskId) {
    String sql = """
      DELETE FROM tasknote.task_url
      WHERE task_id = :taskId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("taskId", taskId);

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Find all Task URLs given a Task ID.
   *
   * @param taskId The Task ID to search by.
   * @return List of TaskUrl found.
   */
  public List<TaskUrl> findAllById_taskId(Long taskId) {
    String sql = """
      SELECT task_id, url
      FROM tasknote.task_url
      WHERE task_id = :taskId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("taskId", taskId);

    return jdbcTemplate.query(sql, params, new TaskUrlRowMapper());
  }
}

class TaskUrlRowMapper implements org.springframework.jdbc.core.RowMapper<TaskUrl> {
  @Override
  public TaskUrl mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    TaskUrlPk pk = new TaskUrlPk(rs.getLong("task_id"), rs.getString("url"));
    return new TaskUrl(pk);
  }
}
