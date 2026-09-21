package br.com.tasknoteapp.server.response;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.Tag;
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
   * Creates a NoteResponse given a Note and its Urals.
   *
   * @param entity The Note source data.
   * @param url The URL associated with the note.
   * @return NoteResponse instance with all note data and URLs, if any.
   */
  public static NoteResponse fromEntity(Note entity, String url, List<Tag> tags) {
    String timeAgoFmt = TimeAgoUtil.format(entity.lastUpdate());

    return new NoteResponse(
        entity.id(),
        entity.title(),
        entity.description(),
        url,
        timeAgoFmt,
        tags.stream().map((t) -> t.name()).toList(),
        entity.shared(),
        entity.shareToken(),
        entity.archived());
  }
}
