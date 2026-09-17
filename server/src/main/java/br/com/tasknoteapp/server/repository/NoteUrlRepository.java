package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.NoteUrl;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** This interface represents a note url repository, for database access. */
@Repository 
public class NoteUrlRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public NoteUrlRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Save a Note URL (Insert or Update)
   * 
   * @param noteUrl A Note URL instance with all props.
   * @return Created or Updated note url instance.
   */
  public NoteUrl save(NoteUrl noteUrl) {
    return noteUrl.getId() == null
      ? create(noteUrl)
      : update(noteUrl);
  }

  private NoteUrl create(NoteUrl noteUrl) {
    String sql = """
      INSERT INTO tasknote.note_urls (note_id, url)
      VALUES (:noteId, :url)
      RETURNING id, note_id, url
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("noteId", noteUrl.getNoteId())
      .addValue("url", noteUrl.getUrl());

    return jdbcTemplate.queryForObject(sql, params, new NoteUrlRowMapper());
  }

  private NoteUrl update(NoteUrl noteUrl) {
    String sql = """
      UPDATE tasknote.note_urls
      SET url = :url
      WHERE id = :id
        AND note_id = :noteId
      RETURNING id, note_id, url
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("id", noteUrl.getId())
      .addValue("noteId", noteUrl.getNoteId())
      .addValue("url", noteUrl.getUrl());

    return jdbcTemplate.queryForObject(sql, params, new NoteUrlRowMapper());
  }

  /**
   * Find a Note URL by Note ID.
   *
   * @param noteId The Note ID to find for.
   * @return Optional of NoteUrl. Can be empty.
   */
  public Optional<NoteUrl> findByNoteId(Long noteId) {
    String sql = """
      SELECT id, note_id, url
      FROM tasknote.note_urls
      WHERE note_id = :noteId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("noteId", noteId);
    
    List<NoteUrl> list = jdbcTemplate.query(sql, params, new NoteUrlRowMapper());
    return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
  }

  /**
   * Find all Note URLs given a list of Notes ID.
   * 
   * @param noteIds List of Note IDs to search with.
   * @return List of NoteUrl with results.
   */
  public List<NoteUrl> findAllByNoteIdList(List<Long> noteIds) {
    String sql = """
      SELECT id, note_id, url
      FROM tasknote.note_urls
      WHERE note_id IN (:noteIds)
    """;
    
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("noteIds", noteIds);
    
    return jdbcTemplate.query(sql, params, new NoteUrlRowMapper());
  }

  /**
   * Deletes all Note URLs given a Note ID.
   * 
   * @param noteId The Note ID to delete for.
   * @return Number of deleted records.
   */
  public int deleteByNoteId(Long noteId) {
    String sql = """
      DELETE FROM tasknote.note_urls
      WHERE note_id = :noteId
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("noteId", noteId);

    return jdbcTemplate.update(sql, params);
  }
}

class NoteUrlRowMapper implements org.springframework.jdbc.core.RowMapper<NoteUrl> {
  @Override
  public NoteUrl mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new NoteUrl(
        rs.getLong("id"),
        rs.getLong("note_id"),
        rs.getString("url")
    );
  }
}
