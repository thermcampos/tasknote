package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.NoteUrl;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This class represents a note url repository, for database access. */
@Repository
public class NoteUrlRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public NoteUrlRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save all Note URLs (insert).
   *
   * @param noteUrls List of NoteUrl instances to be created.
   * @return Number of created records.
   */
  public int saveAll(List<NoteUrl> noteUrls) {
    if (noteUrls.isEmpty()) {
      return 0;
    }

    String sql =
        """
        INSERT INTO tasknote.note_urls (note_id, url)
        VALUES (:noteId, :url)
        """;

    MapSqlParameterSource[] params = noteUrls.stream()
        .map(noteUrl -> new MapSqlParameterSource()
            .addValue("noteId", noteUrl.noteId())
            .addValue("url", noteUrl.url()))
        .toArray(MapSqlParameterSource[]::new);

    return Arrays.stream(jdbcTemplate.batchUpdate(sql, params)).sum();
  }

  /**
   * Find all Note URLs given a Note ID.
   *
   * @param noteId The Note ID to search by.
   * @return List of NoteUrl found.
   */
  public List<NoteUrl> findAllByNoteId(Long noteId) {
    String sql =
        """
        SELECT note_id, url
        FROM tasknote.note_urls
        WHERE note_id = :noteId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("noteId", noteId);

    return jdbcTemplate.query(sql, params, new NoteUrlRowMapper());
  }

  /**
   * Delete all Note URLs given a Note ID.
   *
   * @param noteId The Note ID to delete for.
   * @return Number of deleted records.
   */
  public int deleteAllByNoteId(Long noteId) {
    String sql =
        """
        DELETE FROM tasknote.note_urls
        WHERE note_id = :noteId
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("noteId", noteId);

    return jdbcTemplate.update(sql, params);
  }

  /**
   * Delete a Note URL by its Note ID and URL.
   *
   * @param noteId The Note ID to delete for.
   * @param url The URL to delete.
   * @return Number of deleted records.
   */
  public int deleteByNoteIdAndUrl(Long noteId, String url) {
    String sql =
        """
        DELETE FROM tasknote.note_urls
        WHERE note_id = :noteId
          AND url = :url
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("noteId", noteId)
        .addValue("url", url);

    return jdbcTemplate.update(sql, params);
  }

  class NoteUrlRowMapper implements RowMapper<NoteUrl> {
    @Override
    public NoteUrl mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new NoteUrl(rs.getLong("note_id"), rs.getString("url"));
    }
  }
}
