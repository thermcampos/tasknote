package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Note;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a note repository, for database access. */
@Repository 
public class NoteRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public NoteRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Saves a note to the database and returns the saved note with its generated ID.
   * 
   * @param note the note to be saved.
   * @return the saved note with its generated ID.
   */
  public Note save(Note note) {
    return note.getId() == null
        ? create(note)
        : update(note);
  }

  private Note create(Note note) {
    String sql =
        """
        INSERT INTO tasknote.notes (
          user_id,
          description,
          title,
          last_update,
          shared,
          share_token,
          archived
        ) VALUES (
          :userId,
          :description,
          :title,
          :lastUpdate,
          :shared,
          :shareToken,
          :archived
        )
        RETURNING id, user_id, description, title, last_update, shared, share_token, archived
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", note.getUserId())
        .addValue("description", note.getDescription())
        .addValue("title", note.getTitle())
        .addValue("lastUpdate", note.getLastUpdate())
        .addValue("shared", note.isShared())
        .addValue("shareToken", note.getShareToken())
        .addValue("archived", note.isArchived());

    return jdbcTemplate.queryForObject(sql, params, new NoteRowMapper());
  }

  private Note update(Note note) {
    String sql =
        """
        UPDATE tasknote.notes
        SET description = :description,
          title = :title,
          last_update = :lastUpdate,
          shared = :shared,
          share_token = :shareToken,
          archived = :archived
        WHERE id = :id
          AND user_id = :userId
        RETURNING id, user_id, description, title, last_update, shared, share_token, archived
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("description", note.getDescription())
        .addValue("title", note.getTitle())
        .addValue("lastUpdate", note.getLastUpdate())
        .addValue("shared", note.isShared())
        .addValue("shareToken", note.getShareToken())
        .addValue("archived", note.isArchived())
        .addValue("id", note.getId())
        .addValue("userId", note.getUserId());

    return jdbcTemplate.queryForObject(sql, params, new NoteRowMapper());
  }

  /**
   * Deletes a note from the database.
   * 
   * @param note the note to be deleted.
   * @return the number of rows affected by the delete operation.
   */
  public int delete(Note note) {
    String sql = "DELETE FROM tasknote.notes WHERE id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", note.getId());

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Finds a note by its ID.
   * 
   * @param id the ID of the note to find.
   * @return the note with the specified ID, or empty if not found.
   */
  public Optional<Note> findById(Long id) {
    String sql =
        """
        SELECT id, user_id, description, title, last_update, shared, share_token, archived
        FROM tasknote.notes
        WHERE id = :id
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", id);

    Note note = jdbcTemplate.queryForObject(sql, params, new NoteRowMapper());
    return Optional.ofNullable(note);
  }

  /**
   * Find all notes by user id.
   * 
   * @param userId the User ID to find the notes.
   * @return List of Notes found.
   */
  public List<Note> findAllByUserId(Long userId) {
    String sql =
        """
        SELECT id, user_id, description, title, last_update, shared, share_token, archived
        FROM tasknote.notes
        WHERE user_id = :userId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId);

    return jdbcTemplate.query(sql, params, new NoteRowMapper());
  }

  /**
   * Find a note by the share token.
   * 
   * @param shareToken the token created when sharing the note.
   * @return Optional of Note, could be empty.
   */
  public Optional<Note> findByShareToken(String shareToken) {
    String sql =
        """
        SELECT id, user_id, description, title, last_update, shared, share_token, archived
        FROM tasknote.notes
        WHERE share_token = :shareToken
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("shareToken", shareToken);

    Note note = jdbcTemplate.queryForObject(sql, params, new NoteRowMapper());
    return Optional.ofNullable(note);
  }

  /**
   * Find a note by User ID and Note ID.
   *
   * @param id The Note ID
   * @param userId The User ID
   * @return Optional of Note, can be empty.
   */
  public Optional<Note> findByIdAndUserId(Long id, Long userId) {
    String sql =
        """
        SELECT id, user_id, description, title, last_update, shared, share_token, archived
        FROM tasknote.notes
        WHERE id = :id AND user_id = :userId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", id)
        .addValue("userId", userId);

    Note note = jdbcTemplate.queryForObject(sql, params, new NoteRowMapper());
    return Optional.ofNullable(note);
  }

  /**
   * Find all notes by search term.
   * 
   * @param userId The User ID.
   * @param searchTerm The term to search the notes
   */
  public List<Note> findAllBySearchTerm(Long userId, String searchTerm) {
    String sql =
        """
        SELECT id, user_id, description, title, last_update, shared, share_token, archived
        FROM tasknote.notes
        WHERE user_id = :userId
          AND (
            upper(title) like upper(concat('%', :searchTerm, '%'))
            OR upper(description) like upper(concat('%', :searchTerm, '%'))
          )
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("searchTerm", searchTerm);

    return jdbcTemplate.query(sql, params, new NoteRowMapper());
  }

  class NoteRowMapper implements org.springframework.jdbc.core.RowMapper<Note> {
    @Override
    public Note mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new Note(
          rs.getLong("id"),
          rs.getLong("user_id"),
          rs.getString("description"),
          rs.getString("title"),
          rs.getObject("last_update", java.time.LocalDateTime.class),
          rs.getBoolean("shared"),
          rs.getString("share_token"),
          rs.getBoolean("archived")
      );
    }
  }
}

