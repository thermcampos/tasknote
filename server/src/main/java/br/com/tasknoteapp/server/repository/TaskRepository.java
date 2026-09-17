package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a task repository, for database access. */
@Repository 
public class TaskRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public TaskRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save a task (create or update).
   * 
   * @param task The task instance to be created or updated.
   * @return The new task instance.
   */
  public Task save(Task task) {
    return task.getId() == null
        ? create(task)
        : update(task);
  }

  private Task create(Task task) {
    String sql = """
      INSERT INTO tasknote.tasks (
        user_id, description, completed, last_update, due_date, due_date_notify,
        due_date_notify_sent, high_priority)
      VALUES (
        :userId, :description, :completed, :lastUpdate, :dueDate, :dueDateNotify,
        :dueDateNotifySent, :highPriority)
      RETURNING id, user_id, description, completed, last_update, due_date,
        due_date_notify, due_date_notify_sent, high_priority
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", task.getUserId())
        .addValue("description", task.getDescription())
        .addValue("completed", task.isCompleted())
        .addValue("lastUpdate", task.getLastUpdate())
        .addValue("dueDate", task.getDueDate())
        .addValue("dueDateNotify", task.isDueDateNotify())
        .addValue("dueDateNotifySent", task.isDueDateNotifySent())
        .addValue("highPriority", task.isHighPriority());

    return jdbcTemplate.queryForObject(sql, params, new TaskRowMapper());
  }

  private Task update(Task task) {
    String sql = """
      UPDATE tasknote.tasks
      SET
        user_id = :userId,
        description = :description,
        completed = :completed,
        last_update = :lastUpdate,
        due_date = :dueDate,
        due_date_notify = :dueDateNotify,
        due_date_notify_sent = :dueDateNotifySent,
        high_priority = :highPriority
      WHERE id = :id
      RETURNING id, user_id, description, completed, last_update, due_date,
        due_date_notify, due_date_notify_sent, high_priority
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", task.getUserId())
        .addValue("description", task.getDescription())
        .addValue("completed", task.isCompleted())
        .addValue("lastUpdate", task.getLastUpdate())
        .addValue("dueDate", task.getDueDate())
        .addValue("dueDateNotify", task.isDueDateNotify())
        .addValue("dueDateNotifySent", task.isDueDateNotifySent())
        .addValue("highPriority", task.isHighPriority())
        .addValue("id", task.getId());

    return jdbcTemplate.queryForObject(sql, params, new TaskRowMapper());
  }

  /**
   * Delete a task given a Task instance (needs the ID).
   *
   * @param task The Task instance to be deleted containing the ID.
   * @return The number of affected rows.
   */
  public int delete(Task task) {
    String sql = "DELETE FROM tasknote.tasks WHERE id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", task.getId());

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Find all Tasks given a User ID.
   *
   * @param userId The User ID to fetch by.
   * @return The list of Tasks found.
   */
  public List<Task> findAllByUserId(Long userId) {
    String sql = """
      SELECT id, user_id, description, completed, last_update, due_date, due_date_notify, due_date_notify_sent, high_priority
      FROM tasknote.tasks
      WHERE user_id = :userId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.query(sql, params, new TaskRowMapper());
  }

  /**
   * Find all Tasks given a Task ID and a User ID.
   *
   * @param id The Task ID to find by.
   * @param userId The User ID to find by
   * @return Optional of a Task instance.
   */
  public Optional<Task> findByIdAndUserId(Long id, Long userId) {
    String sql = """
      SELECT id, user_id, description, completed, last_update, due_date, due_date_notify, due_date_notify_sent, high_priority
      FROM tasknote.tasks
      WHERE user_id = :userId
        AND id = :id
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("id", id);

    List<Task> tasks = jdbcTemplate.query(sql, params, new TaskRowMapper());
    return tasks.stream().findFirst();
  }

  /**
   * Find all Tasks given a User ID and a Search text.
   *
   * @param searchTerm The Text to be searched by.
   * @param userId The User ID to be search by.
   * @return List of Tasks.
   */
  public List<Task> findAllBySearchTerm(String searchTerm, Long userId) {
    String sql = """
      SELECT DISTINCT t.*
      FROM tasknote.tasks t
      LEFT JOIN tasknote.task_url tu ON tu.task_id = t.id
      LEFT JOIN tasknote.task_tags tt ON tt.task_id = t.id
      LEFT JOIN tasknote.tags tg ON tg.id = tt.tag_id
      WHERE (
        UPPER(t.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
        OR UPPER(tg.name) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
        OR UPPER(tu.url) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
        )
        AND t.user_id = :userId
        AND t.completed = false
    """;
    
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("searchTerm", searchTerm);

    return jdbcTemplate.query(sql, params, new TaskRowMapper());
  }
}

class TaskRowMapper implements org.springframework.jdbc.core.RowMapper<Task> {
  @Override
  public Task mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new Task(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("description"),
        rs.getBoolean("completed"),
        rs.getObject("last_update", LocalDateTime.class),
        rs.getObject("due_date", LocalDate.class),
        rs.getBoolean("due_date_notify"),
        rs.getBoolean("due_date_notify_sent"),
        rs.getBoolean("high_priority")
    );
  }
}