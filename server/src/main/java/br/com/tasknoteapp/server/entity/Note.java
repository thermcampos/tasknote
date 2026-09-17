package br.com.tasknoteapp.server.entity;

import java.time.LocalDateTime;

/** This class represents a note in the database. */
public class Note {

  private Long id;
  private Long userId;
  private String description;
  private String title;
  private LocalDateTime lastUpdate;
  private Boolean shared;
  private String shareToken;
  private Boolean archived;

  public Note() {}

  public Note(Long id, Long userId, String description, String title, LocalDateTime lastUpdate, Boolean shared, String shareToken, Boolean archived) {
    this.id = id;
    this.userId = userId;
    this.description = description;
    this.title = title;
    this.lastUpdate = lastUpdate;
    this.shared = shared;
    this.shareToken = shareToken;
    this.archived = archived;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDateTime getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(LocalDateTime lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public Boolean isShared() {
    return shared;
  }

  public void setShared(Boolean shared) {
    this.shared = shared;
  }

  public String getShareToken() {
    return shareToken;
  }

  public void setShareToken(String shareToken) {
    this.shareToken = shareToken;
  }

  public Boolean isArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Note that = (Note) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Note{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", description='"
        + description
        + '\''
        + ", lastUpdate="
        + lastUpdate
        + ", archived="
        + archived
        + '}';
  }
}
