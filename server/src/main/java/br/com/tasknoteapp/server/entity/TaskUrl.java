package br.com.tasknoteapp.server.entity;

/** This class represents a task url in the database. */
public class TaskUrl {

  private TaskUrlPk id;

  public TaskUrl() {}

  public TaskUrl(TaskUrlPk id) {
    this.id = id;
  }

  public TaskUrlPk getId() {
    return id;
  }

  public void setId(TaskUrlPk id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskUrl that = (TaskUrl) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "TaskUrlEntity{" + "id=" + id + '}';
  }
}
