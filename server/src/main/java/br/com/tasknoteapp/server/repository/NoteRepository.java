package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Note;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a note repository, for database access. */
@Repository 
public class NoteRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final long homeWindowMinutes;

  public NoteRepository(
      NamedParameterJdbcTemplate jdbcTemplate,
      @Value ("${br.com.tasknote.server.home-window-minutes:1440}") long homeWindowMinutes) {
    this.jdbcTemplate = jdbcTemplate;
    this.homeWindowMinutes = homeWindowMinutes;
  }

  /**
   * Saves a note to the database and returns the saved note with its generated ID.
   * 
   * @param note the note to be saved.
   * @return the saved note with its generated ID.
   */
  public Note save(Note note) {
    return note.id() == null
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
        .addValue("userId", note.userId())
        .addValue("description", note.description())
        .addValue("title", note.title())
        .addValue("lastUpdate", note.lastUpdate())
        .addValue("shared", note.shared())
        .addValue("shareToken", note.shareToken())
        .addValue("archived", note.archived());

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
        .addValue("description", note.description())
        .addValue("title", note.title())
        .addValue("lastUpdate", note.lastUpdate())
        .addValue("shared", note.shared())
        .addValue("shareToken", note.shareToken())
        .addValue("archived", note.archived())
        .addValue("id", note.id())
        .addValue("userId", note.userId());

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
        .addValue("id", note.id());

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

    return jdbcTemplate.query(sql, params, new NoteRowMapper()).stream().findFirst();
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

    return jdbcTemplate.query(sql, params, new NoteRowMapper()).stream().findFirst();
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

    return jdbcTemplate.query(sql, params, new NoteRowMapper()).stream().findFirst();
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

  /**
   * Find Notes for the Home view: either the default window (touched in the last 24h) or an
   * unbounded search/tag query.
   *
   * @param userId The User ID to fetch by.
   * @param searchTerm Optional text to search in title, description, url and tags.
   * @param tag Optional tag name to filter by ("untagged" matches notes without tags).
   * @param windowedOnly When true, restrict to notes touched in the last 24h.
   * @return List of Notes found.
   */
  public List<Note> findHomeNotes(
      Long userId, String searchTerm, String tag, boolean windowedOnly) {
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT DISTINCT n.id,
              n.user_id,
              n.description,
              n.title,
              n.last_update,
              n.shared,
              n.share_token,
              n.archived
            FROM tasknote.notes n
            LEFT JOIN tasknote.note_tags nt ON nt.note_id = n.id
            LEFT JOIN tasknote.tags tg ON tg.id = nt.tag_id
            LEFT JOIN tasknote.note_urls nu ON nu.note_id = n.id
            WHERE n.user_id = :userId
            """);

    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);

    if (windowedOnly) {
      sql.append("\n  AND n.last_update >= :windowStart\n");
      params.addValue(
          "windowStart", java.time.LocalDateTime.now().minusMinutes(homeWindowMinutes));
    } else {
      if (searchTerm != null && !searchTerm.isBlank()) {
        sql.append(
            """
            \
              AND (
                UPPER(n.title) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
                OR UPPER(n.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
                OR UPPER(nu.url) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
                OR UPPER(tg.name) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
              )
            """);
        params.addValue("searchTerm", searchTerm);
      }
      if (tag != null && !tag.isBlank()) {
        if ("untagged".equals(tag)) {
          sql.append(
              """
              \
                AND NOT EXISTS (
                  SELECT 1 FROM tasknote.note_tags nt2 WHERE nt2.note_id = n.id
                )
              """);
        } else {
          sql.append("\n  AND tg.name = :tag\n");
          params.addValue("tag", tag);
        }
      }
    }

    return jdbcTemplate.query(sql.toString(), params, new NoteRowMapper());
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

