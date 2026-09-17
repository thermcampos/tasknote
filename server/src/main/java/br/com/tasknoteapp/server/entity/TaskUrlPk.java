package br.com.tasknoteapp.server.entity;

/** This class represents a UrlTaskEntity primary key. */
public class TaskUrlPk {

  private Long taskId;
  private String url;

  public TaskUrlPk() {}

  public TaskUrlPk(Long taskId, String url) {
    this.taskId = taskId;
    this.url = url;
  }

  public Long getTaskId() {
    return taskId;
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
    TaskUrlPk that = (TaskUrlPk) o;
    return url.equals(that.url) && taskId.equals(that.taskId);
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (url != null ? url.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TaskUrlEntityPk{" + "taskId=" + taskId + ", url='" + url + '\'' + '}';
  }
}
