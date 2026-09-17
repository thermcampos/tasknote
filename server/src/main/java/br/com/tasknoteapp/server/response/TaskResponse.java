package br.com.tasknoteapp.server.response;

import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.util.TimeAgoUtil;
import java.time.LocalDate;
import java.util.List;

/** This record represents a task and its urls object to be returned. */
public record TaskResponse(
    Long id,
    Boolean completed,
    String description,
    Boolean highPriority,
    LocalDate dueDate,
    String dueDateFmt,
    String lastUpdate,
    List<String> tags,
    List<String> urls) {

  /**
   * Creates a TaskResponse given a TaskEntity and its URLs.
   *
   * @param entity The TaskEntity source data.
   * @param urls The URLs associated with the task.
   * @return TaskResponse instance with all task data and URLs, if any.
   */
  public static TaskResponse fromEntity(Task entity, List<String> urls, List<Tag> tags) {
    String timeAgoFmt = TimeAgoUtil.format(entity.getLastUpdate());
    String dueDateFmt = TimeAgoUtil.formatDueDate(entity.getDueDate());

    return new TaskResponse(
        entity.getId(),
        entity.isCompleted(),
        entity.getDescription(),
        entity.isHighPriority(),
        entity.getDueDate(),
        dueDateFmt,
        timeAgoFmt,
        tags.stream().map((t) -> t.getName()).toList(),
        urls);
  }
}
