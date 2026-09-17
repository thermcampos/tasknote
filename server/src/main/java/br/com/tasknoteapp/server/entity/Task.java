package br.com.tasknoteapp.server.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** This class represents a task in the database. */
public class Task {

  private Long id;
  private Long userId;
  private String description;
  private Boolean completed;
  private LocalDateTime lastUpdate;
  private LocalDate dueDate;
  private Boolean dueDateNotify;
  private Boolean dueDateNotifySent;
  private Boolean highPriority;

  public Task() {}

  public Task(Long id, Long userId, String description, Boolean completed, LocalDateTime lastUpdate, LocalDate dueDate, Boolean dueDateNotify, 
    Boolean dueDateNotifySent, Boolean highPriority) {
      this.id = id;
      this.userId = userId;
      this.description = description;
      this.completed = completed;
      this.lastUpdate = lastUpdate;
      this.dueDate = dueDate;
      this.dueDateNotify = dueDateNotify;
      this.dueDateNotifySent = dueDateNotifySent;
      this.highPriority = highPriority;
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

  public Boolean isCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public LocalDateTime getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(LocalDateTime lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public Boolean isDueDateNotify() {
    return dueDateNotify;
  }

  public void setDueDateNotify(Boolean dueDateNotify) {
    this.dueDateNotify = dueDateNotify;
  }

  public Boolean isDueDateNotifySent() {
    return dueDateNotifySent;
  }

  public void setDueDateNotifySent(Boolean dueDateNotifySent) {
    this.dueDateNotifySent = dueDateNotifySent;
  }

  public Boolean isHighPriority() {
    return highPriority;
  }

  public void setHighPriority(Boolean highPriority) {
    this.highPriority = highPriority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Task that = (Task) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Task{");
    sb.append("id=").append(id).append(", ");
    sb.append("userId=").append(userId).append(", ");
    sb.append("description=\"").append(description).append("\", ");
    sb.append("completed=").append(completed).append(", ");
    sb.append("lastUpdate=\"").append(lastUpdate).append("\", ");
    sb.append("dueDate=\"").append(dueDate).append("\", ");
    sb.append("dueDateNotify=").append(dueDateNotify).append(", ");
    sb.append("dueDateNotifySent=").append(dueDateNotifySent).append(", ");
    sb.append("highPriority=").append(highPriority).append("}");
    return sb.toString();
  }
}
