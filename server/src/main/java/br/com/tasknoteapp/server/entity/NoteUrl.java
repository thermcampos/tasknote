package br.com.tasknoteapp.server.entity;

/** This class represents a note url in the database. */
public class NoteUrl {

  private Long id;
  private Long noteId;
  private String url;

  public NoteUrl() {}

  public NoteUrl(Long id, Long noteId, String url) {
    this.id = id;
    this.noteId = noteId;
    this.url = url;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long noteId) {
    this.noteId = noteId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoteUrl that = (NoteUrl) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "NoteUrl{" + "id=" + id + ", url='" + url + '\'' + ", noteId=" + noteId + '}';
  }
}
