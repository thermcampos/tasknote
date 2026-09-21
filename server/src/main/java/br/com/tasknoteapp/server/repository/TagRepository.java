package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.TaskNoteTag;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a tag repository, for database access. */
@Repository 
public class TagRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public TagRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Saves a Tag (create or update) given an existing Tag ID in the instance.
   * 
   * @param tag The Tag instance to be created or updated.
   * @return Tag created or updated.
   */
  public Tag save(Tag tag, String source, Long id) {
    return tag.id() == null
        ? create(tag, source, id)
        : update(tag);
  }

  private Tag create(Tag tag, String source, Long id) {
    String sql =
        """
        INSERT INTO tasknote.tags (name, user_id)
        VALUES (:name, :userId)
        RETURNING id, name, user_id
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("name", tag.name())
        .addValue("userId", tag.userId());

    final Tag tagCreated = jdbcTemplate.queryForObject(sql, params, new TagRowMapper());

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO tasknote.");
    if ("tasks".equals(source)) {
      sb.append("task_tags (task_id");
    } else if ("notes".equals(source)) {
      sb.append("note_tags (note_id");
    }
    sb.append(", tag_id) VALUES (:outerId, :tagId)");

    MapSqlParameterSource relationParams = new MapSqlParameterSource()
        .addValue("outerId", id)
        .addValue("tagId", tagCreated.id());

    jdbcTemplate.update(sb.toString(), relationParams);

    return tagCreated;
  }

  private Tag update(Tag tag) {
    String sql =
        """
        UPDATE tasknote.tags
        SET name = :name
        WHERE id = :id
          and user_id = :userId
        RETURNING id, name, user_id
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("name", tag.name())
        .addValue("id", tag.id())
        .addValue("userId", tag.userId());

    return jdbcTemplate.queryForObject(sql, params, new TagRowMapper());
  }

  /**
   * Update the relationship for a tag and a task.
   *
   * @param tag The Tag instance to be updated.
   * @param taskId The Task ID to be updated.
   */
  public void updateTagForTask(Tag tag, Long taskId) {
    String sql =
        """
        INSERT INTO tasknote.task_tags (task_id, tag_id)
        VALUES (:taskId, :tagId)
        ON CONFLICT DO NOTHING;
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("taskId", taskId)
        .addValue("tagId", tag.id());

    jdbcTemplate.update(sql, params);
  }

  /**
   * Delete the tag x task relashionship for a tag ID lists.
   *
   * @param tagId Tag ID
   * @param taskId Task ID
   * @return Number of affected rows.
   */
  public int deleteTagFromTask(List<Long> tagIds, Long taskId) {
    String sql =
        """
        DELETE FROM tasknote.task_tags
        WHERE task_id = :taskId
          AND tag_id IN (:tagIds)
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("taskId", taskId)
        .addValue("tagIds", tagIds);

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Find all Tags given a User ID and a Task ID.
   * 
   * @param userId The User ID to search by.
   * @param taskId The Note ID to search by.
   * @return List of Tags found.
   */
  public List<Tag> findAllByUserIdAndTaskId(Long userId, Long taskId) {
    String sql =
        """
        SELECT ta.id, ta.name, ta.user_id
        FROM tasknote.tags ta
        JOIN tasknote.task_tags nt ON nt.tag_id = ta.id
        JOIN tasknote.task t ON t.id = nt.note_id
        WHERE ta.user_id = :userId
          AND t.id = :noteId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("taskId", taskId);

    return jdbcTemplate.query(sql, params, new TagRowMapper());
  }

  /**
   * Query all notes and tasks tags relationship looking for tags.
   *
   * @param userId The User ID to fetch by.
   * @return true if user has any tags, false otherwise.
   */
  public boolean userHasAnyTags(Long userId) {
    String sql =
        """
        SELECT DISTINCT ta.id, ta.name, ta.user_id, tt.tag_id AS taskNoteId
        FROM tasknote.tags ta
        JOIN tasknote.task_tags tt ON tt.tag_id = ta.id
        JOIN tasknote.note_tags nt ON nt.tag_id = ta.id
        WHERE ta.user_id = :userId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    List<TaskNoteTag> allTags = jdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> {
      return new TaskNoteTag(
          rs.getLong("id"),
          rs.getString("name"),
          rs.getLong("user_id"),
          rs.getLong("taskNoteId")
      );
    });

    return !allTags.isEmpty();
  }

  /**
   * Find all Tags given a User ID and a list of Task IDs.
   *
   * @param userId The User ID to find by.
   * @param taskIdList The List of Task ID.
   * @return List of found tags.
   */
  public List<TaskNoteTag> findAllByUserIdAndTaskIdInList(Long userId, List<Long> taskIdList) {
    String sql =
        """
        SELECT DISTINCT ta.id, ta.name, ta.user_id, tt.task_id AS taskNoteId
        FROM tasknote.tags ta
        JOIN tasknote.task_tags tt ON tt.tag_id = ta.id
        WHERE ta.user_id = :userId
          AND tt.task_id IN (:taskIdList)
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("taskIdList", taskIdList);

    return jdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> {
      return new TaskNoteTag(
          rs.getLong("id"),
          rs.getString("name"),
          rs.getLong("user_id"),
          rs.getLong("taskNoteId")
      );
    });
  }

  /**
   * Find all Tags given a User ID and a list of Task IDs.
   *
   * @param userId The User ID to find by.
   * @param noteIdList The List of Task ID.
   * @return List of found tags.
   */
  public List<TaskNoteTag> findAllByUserIdAndNoteIdInList(Long userId, List<Long> noteIdList) {
    String sql =
        """
        SELECT DISTINCT ta.id, ta.name, ta.user_id, nt.note_id AS taskNoteId
        FROM tasknote.tags ta
        JOIN tasknote.note_tags nt ON nt.tag_id = ta.id
        WHERE ta.user_id = :userId
          AND nt.note_id IN (:noteIdList)
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("noteIdList", noteIdList);

    return jdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> {
      return new TaskNoteTag(
          rs.getLong("id"),
          rs.getString("name"),
          rs.getLong("user_id"),
          rs.getLong("taskNoteId")
      );
    });
  }

  /**
   * Find a Tag given a User ID and the Tag name.
   * 
   * @param userId The User ID to search by.
   * @param name The tag name to search by.
   * @return Optional of Tag.
   */
  public Optional<Tag> findByUserIdAndName(Long userId, String name) {
    String sql =
        """
        SELECT id, name, user_id
        FROM tasknote.tags
        WHERE user_id = :userId
          AND name = :name
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("name", name);

    List<Tag> tags = jdbcTemplate.query(sql, params, new TagRowMapper());

    return tags.stream().findFirst();
  }

  /**
   * Deletes all orphaned tags given a User ID (deleting from
   * both Notes and Tasks).
   * 
   * @param userId The User ID to search by.
   * @return Number of deleted rows.
   */
  public int deleteOrphanedTags(Long userId) {
    String sql =
        """
        DELETE FROM tasknote.tags t
        WHERE t.user_id = :userId
          AND NOT EXISTS (SELECT 1 FROM tasknote.task_tags tk WHERE tk.tag_id = t.id)
          AND NOT EXISTS (SELECT 1 FROM tasknote.note_tags nt WHERE nt.tag_id = t.id)
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Deletes all Tags given a User ID.
   * 
   * @param userId The User ID to search by.
   * @return Number of deleted rows.
   */
  public int deleteAllForUser(Long userId) {
    String sql =
        """
        DELETE FROM tasknote.tags t
        WHERE t.user_id = :userId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.update(sql, params);
  }

  class TagRowMapper implements org.springframework.jdbc.core.RowMapper<Tag> {
    @Override
    public Tag mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new Tag(
          rs.getLong("id"),
          rs.getString("name"),
          rs.getLong("user_id")
      );
    }
  }
}
