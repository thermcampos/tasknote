package br.com.tasknoteapp.server.response;

import br.com.tasknoteapp.server.entity.NoteEntity;
import br.com.tasknoteapp.server.entity.TagEntity;
import br.com.tasknoteapp.server.util.TimeAgoUtil;
import java.util.List;

/** This record represents a task and its URLs object to be returned. */
public record NoteResponse(
    Long id,
    String title,
    String description,
    String url,
    String lastUpdate,
    List<String> tags,
    boolean shared,
    String shareToken,
    boolean archived) {

  /**
   * Creates a NoteResponse given a NoteEntity and its Urals.
   *
   * @param entity The NoteEntity source data.
   * @param url The URL associated with the note.
   * @return NoteResponse instance with all note data and URLs, if any.
   */
  public static NoteResponse fromEntity(NoteEntity entity, String url) {
    String timeAgoFmt = TimeAgoUtil.format(entity.getLastUpdate());

    return new NoteResponse(
        entity.getId(),
        entity.getTitle(),
        entity.getDescription(),
        url,
        timeAgoFmt,
        entity.getTags().stream().map(TagEntity::getName).toList(),
        entity.isShared(),
        entity.getShareToken(),
        entity.isArchived());
  }
}
