package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Tag;
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
    return tag.getId() == null
        ? create(tag, source, id)
        : update(tag);
  }

  private Tag create(Tag tag, String source, Long id) {
    String sql = """
      INSERT INTO tasknote.tags (name, user_id)
      VALUES (:name, :userId)
      RETURNING id, name, user_id
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", tag.getName())
                .addValue("userId", tag.getUserId());

    Tag tagCreated = jdbcTemplate.queryForObject(sql, params, new TagRowMapper());

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
                .addValue("tagId", tagCreated.getId());

    jdbcTemplate.update(sb.toString(), relationParams);

    return tagCreated;
  }

  private Tag update(Tag tag) {
    String sql = """
      UPDATE tasknote.tags
      SET name = :name
      WHERE id = :id
        and user_id = :userId
      RETURNING id, name, user_id
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("name", tag.getName())
        .addValue("id", tag.getId())
        .addValue("userId", tag.getUserId());

    return jdbcTemplate.queryForObject(sql, params, new TagRowMapper());
  }

  /**
   * Find all Tags given a User ID and a Note ID.
   * 
   * @param userId The User ID to search by.
   * @param noteId The Note ID to search by.
   * @return List of Tags found.
   */
  public List<Tag> findAllByUserIdAndNoteId(Long userId, Long noteId) {
    String sql = """
      SELECT ta.id, ta.name, ta.user_id
      FROM tasknote.tags ta
      JOIN tasknote.note_tags nt ON nt.tag_id = ta.id
      JOIN tasknote.notes t ON t.id = nt.note_id
      WHERE ta.user_id = :userId
        AND t.id = :noteId

    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("noteId", noteId);

    return jdbcTemplate.query(sql, params, new TagRowMapper());
  }

  /**
   * Find a Tag given a User ID and the Tag name.
   * 
   * @param userId The User ID to search by.
   * @param name The tag name to search by.
   * @return Optional of Tag.
   */
  public Optional<Tag> findByUserIdAndName(Long userId, String name) {
    String sql = """
      SELECT id, name, user_id
      FROM tasknote.tags
      WHERE user_id = :userId
        AND name = :name
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("name", name);

    List<Tag> tags = jdbcTemplate.query(sql, params, new TagRowMapper());
    System.out.println("-- findByUserIdAndName -- tags count: " + tags.size());
    return tags.stream().findFirst();
  }

  /**
   * Find all Tags by User ID ordered by Tag Name in Ascending order.
   * 
   * @param userId
   * @return List of Tags ordered by Tag Name in Ascending order.
   */
  public List<Tag> findAllByUserIdOrderByNameAsc(Long userId) {
    String sql = """
      SELECT id, name, user_id
      FROM tasknote.tags
      WHERE user_id = :userId
      ORDER BY name ASC
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.query(sql, params, new TagRowMapper());
  }

  /**
   * Deletes all orphaned tags given a User ID (deleting from
   * both Notes and Tasks).
   * 
   * @param userId The User ID to search by.
   * @return Number of deleted rows.
   */
  public int deleteOrphanedTags(Long userId) {
    String sql = """
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
    String sql = """
      DELETE FROM tasknote.tags t
      WHERE t.user_id = :userId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.update(sql, params);
  }
}

class TagRowMapper implements org.springframework.jdbc.core.RowMapper<Tag> {
  @Override
  public Tag mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new Tag(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getLong("user_id")
    );
  }
}
