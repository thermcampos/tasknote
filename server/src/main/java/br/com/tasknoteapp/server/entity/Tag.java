package br.com.tasknoteapp.server.entity;

/** This class represents a tag in the database. */
public class Tag {

  private Long id;
  private String name;
  private Long userId;

  public Tag() {}

  public Tag(String name, Long userId) {
    this(null, name, userId);
  }

  public Tag(Long id, String name, Long userId) {
    this.id = id;
    this.name = name;
    this.userId = userId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tag that = (Tag) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "TagEntity{" + "id=" + id + ", name='" + name + '\'' + ", userId=" + userId + '}';
  }
}
