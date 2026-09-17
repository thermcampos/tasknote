package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.entity.TaskUrl;
import br.com.tasknoteapp.server.entity.TaskUrlPk;
import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.exception.TaskNotFoundException;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.repository.TaskRepository;
import br.com.tasknoteapp.server.repository.TaskUrlRepository;
import br.com.tasknoteapp.server.request.TaskPatchRequest;
import br.com.tasknoteapp.server.request.TaskRequest;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** This class contains the implementation for the Task Service class. */
@Service
public class TaskService {

  private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

  private final TaskRepository taskRepository;

  private final AuthService authService;

  private final AuthUtil authUtil;

  private final TaskUrlRepository taskUrlRepository;

  private final TagRepository tagRepository;

  /**
   * Constructor for the TaskService class.
   *
   * @param taskRepository The repository for task entities.
   * @param authService The service for authentication.
   * @param authUtil Utility class for authentication-related operations.
   * @param taskUrlRepository The repository for task URL entities.
   * @param tagRepository The repository for tag entities.
   */
  public TaskService(
      TaskRepository taskRepository,
      AuthService authService,
      AuthUtil authUtil,
      TaskUrlRepository taskUrlRepository,
      TagRepository tagRepository) {
    this.taskRepository = taskRepository;
    this.authService = authService;
    this.authUtil = authUtil;
    this.taskUrlRepository = taskUrlRepository;
    this.tagRepository = tagRepository;
  }

  /**
   * Get all tasks for the current user.
   *
   * @return {@link List} of {@link TaskResponse} with all Tasks found or an empty list.
   */
  @Transactional 
  public List<TaskResponse> getAllTasks() {
    User user = getCurrentUser();
    logger.info("Get all tasks to user ID {}", user.getId());

    List<Task> tasks = taskRepository.findAllByUserId(user.getId());
    logger.info("{} tasks found in getAllTasks!", tasks.size());

    List<Tag> allTags = tagRepository.findAllByUserIdOrderByNameAsc(user.getId());

    return tasks.stream()
        .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), allTags))
        .toList();
  }

  /**
   * Get a task by its id.
   *
   * @param taskId The task id in the database.
   * @return {@link TaskResponse} with the found task or throw a {@link TaskNotFoundException}.
   */
  @Transactional
  public TaskResponse getTaskById(Long taskId) {
    User user = getCurrentUser();
    logger.info("Get task ID {} to user ID {}", taskId, user.getId());

    Optional<Task> task = taskRepository.findByIdAndUserId(taskId, user.getId());
    if (task.isEmpty()) {
      throw new TaskNotFoundException();
    }

    List<Tag> allTags = tagRepository.findAllByUserIdOrderByNameAsc(user.getId());

    logger.info("Task found! ID {}", taskId);
    return TaskResponse.fromEntity(task.get(), getAllTasksUrls(taskId), allTags);
  }

  /**
   * Create a task for the user in the database.
   *
   * @param taskRequest The {@link TaskRequest} containing all task data.
   */
  @Transactional
  public TaskResponse createTask(TaskRequest taskRequest) {
    User user = getCurrentUser();

    logger.info("Creating task to user ID {}", user.getId());

    Task task = new Task();
    task.setDescription(taskRequest.description());
    task.setCompleted(false);
    task.setUserId(user.getId());
    task.setLastUpdate(LocalDateTime.now());
    if (!Objects.isNull(taskRequest.dueDate()) && !taskRequest.dueDate().isBlank()) {
      task.setDueDate(LocalDate.parse(taskRequest.dueDate()));
    }
    task.setHighPriority(taskRequest.highPriority());
    
    Task created = taskRepository.save(task);

    if (!Objects.isNull(taskRequest.tags())) {
      getOrCreateTags(taskRequest.tags(), user, created.getId());
    }

    if (!Objects.isNull(taskRequest.urls()) && !taskRequest.urls().isEmpty()) {
      saveUrls(created, taskRequest.urls());
    }

    tagRepository.deleteOrphanedTags(user.getId());

    List<Tag> allTags = tagRepository.findAllByUserIdOrderByNameAsc(user.getId());

    logger.info("Task created! ID {}", created.getId());
    return TaskResponse.fromEntity(created, getAllTasksUrls(created.getId()), allTags);
  }

  /**
   * Patch a task for the current user.
   *
   * @param taskId The task id from the database.
   * @param patch An instance of {@link TaskPatchRequest} with the content to be patched.
   * @return {@link TaskResponse} with the updated content.
   */
  @Transactional
  public TaskResponse patchTask(Long taskId, TaskPatchRequest patch) {
    User user = getCurrentUser();

    logger.info("Patching task ID {} to user ID {}", taskId, user.getId());

    Optional<Task> task = taskRepository.findByIdAndUserId(taskId, user.getId());
    if (task.isEmpty()) {
      throw new TaskNotFoundException();
    }

    Task taskEntity = task.get();
    if (!Objects.isNull(patch.description()) && !patch.description().isBlank()) {
      taskEntity.setDescription(patch.description().trim());
    }
    if (!Objects.isNull(patch.completed())) {
      taskEntity.setCompleted(patch.completed());
    }

    patchDueDate(taskEntity, patch);

    if (!Objects.isNull(patch.highPriority())) {
      taskEntity.setHighPriority(patch.highPriority());
    }

    taskEntity.setLastUpdate(LocalDateTime.now());

    patchTaskUrl(taskEntity, patch);

    Task patchedTask = taskRepository.save(taskEntity);

    if (!Objects.isNull(patch.tags())) {
      getOrCreateTags(patch.tags(), user, taskId);
    }

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Task patched! ID {}", patchedTask.getId());

    List<Tag> tagList = tagRepository.findAllByUserIdAndNoteId(user.getId(), patchedTask.getId());
    return TaskResponse.fromEntity(patchedTask, getAllTasksUrls(taskId), tagList);
  }

  /**
   * Delete a task from the database.
   *
   * @param taskId The task id in the database
   */
  @Transactional
  public void deleteTask(Long taskId) {
    User user = getCurrentUser();

    logger.info("Deleting task ID {} to user ID {}", taskId, user.getId());

    Optional<Task> task = taskRepository.findByIdAndUserId(taskId, user.getId());
    if (task.isEmpty()) {
      throw new TaskNotFoundException();
    }

    Task taskEntity = task.get();

    List<TaskUrl> urlsToDelete = taskUrlRepository.findAllById_taskId(taskEntity.getId());
    if (!urlsToDelete.isEmpty()) {
      taskUrlRepository.deleteAllById_taskId(taskId);
      logger.info("Deleted {} URLs from task ID {} in deleteTask", urlsToDelete.size(), taskId);
    } else {
      logger.info("No URLs to delete for task ID {} in deleteTask", taskId);
    }

    taskRepository.delete(taskEntity);

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Task deleted! ID {}", taskId);
  }

  /**
   * Search for tasks in the database given a search term.
   *
   * @param searchTerm The term to be used for the search.
   * @return {@link List} of {@link TaskResponse} with found records or an empty list.
   */
  @Transactional
  public List<TaskResponse> searchTasks(String searchTerm) {
    User user = getCurrentUser();

    logger.info("Searching tasks to user ID {}", user.getId());

    if (Objects.isNull(searchTerm) || searchTerm.isBlank()) {
      return List.of();
    }

    List<Task> tasks =
        taskRepository.findAllBySearchTerm(searchTerm.toUpperCase(), user.getId());
    logger.info("{} tasks found!", tasks.size());

    // TODO: review empty tag list
    return tasks.stream()
        .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), List.of()))
        .toList();
  }

  /**
   * Get tasks by a given filter.
   *
   * @param filter The filter to get the tasks.
   * @return {@link List} of {@link TaskResponse} with found records or an empty list.
   */
  @Transactional
  public List<TaskResponse> getTasksByFilter(String filter) {
    User user = getCurrentUser();

    List<Task> allTasks =
        taskRepository.findAllByUserId(user.getId()).stream()
            .filter(t -> t.isCompleted().equals(Boolean.FALSE))
            .toList();
    if (allTasks.isEmpty()) {
      return List.of();
    }

    // TODO: review empty tag list
    return switch (filter) {
      case "all" ->
          allTasks.stream()
              .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), List.of()))
              .toList();
      case "high" ->
          allTasks.stream()
              .filter(((t) -> t.isHighPriority().equals(Boolean.TRUE)))
              .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), List.of()))
              .toList();
      case "untagged" ->
          allTasks.stream()
              //.filter(t -> t.getTags().isEmpty())
              .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), List.of()))
              .toList();
      default ->
          allTasks.stream()
              //.filter(t -> t.getTags().stream().anyMatch(tag -> tag.getName().equals(filter)))
              .map((Task tr) -> TaskResponse.fromEntity(tr, getAllTasksUrls(tr.getId()), List.of()))
              .toList();
    };
  }

  private Set<Tag> getOrCreateTags(List<String> tagNames, User user, Long taskId) {
    if (Objects.isNull(tagNames) || tagNames.isEmpty()) {
      return new HashSet<>();
    }

    Set<String> normalizedNames =
        tagNames.stream()
            .filter(name -> !Objects.isNull(name) && !name.isBlank())
            .map(name -> name.trim().toLowerCase())
            .collect(Collectors.toSet());

    Set<Tag> tags = new HashSet<>();
    for (String name : normalizedNames) {
      Tag tag =
          tagRepository
              .findByUserIdAndName(user.getId(), name)
              .orElseGet(() -> tagRepository.save(new Tag(name, user.getId()), "tasks", taskId));
      tags.add(tag);
    }
    return tags;
  }

  private User getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    return authService.findByEmail(email).orElseThrow();
  }

  private List<String> getAllTasksUrls(Long taskId) {
    List<TaskUrl> urls = taskUrlRepository.findAllById_taskId(taskId);
    return urls.stream().map(TaskUrl::getId).map(TaskUrlPk::getUrl).toList();
  }

  private void saveUrls(Task taskEntity, List<String> urls) {
    List<TaskUrl> tasksUrl = new ArrayList<>();
    for (String url : urls) {
      TaskUrl taskUrl = new TaskUrl();
      TaskUrlPk pk = new TaskUrlPk(taskEntity.getId(), url);
      taskUrl.setId(pk);
      tasksUrl.add(taskUrl);
    }

    taskUrlRepository.saveAll(tasksUrl);
    logger.info("Added {} URLs from task ID {}", tasksUrl.size(), taskEntity.getId());
  }

  private void patchDueDate(Task taskEntity, TaskPatchRequest patch) {
    taskEntity.setDueDate(null);
    if (!Objects.isNull(patch.dueDate()) && !patch.dueDate().isBlank()) {
      try {
        taskEntity.setDueDate(LocalDate.parse(patch.dueDate()));
      } catch (DateTimeParseException e) {
        logger.error("Unable to parse the provided date: {}: {}", patch.dueDate(), e.getMessage());
      }
    }
  }

  private void patchTaskUrl(Task taskEntity, TaskPatchRequest patch) {
    Long taskId = taskEntity.getId();
    List<TaskUrl> urlsToDelete = taskUrlRepository.findAllById_taskId(taskId);
    if (!urlsToDelete.isEmpty()) {
      taskUrlRepository.deleteAllById_taskId(taskId);
      logger.info("Deleted {} URLs from task ID {}", urlsToDelete.size(), taskId);
    } else {
      logger.info("No URLs to delete for task ID {}", taskId);
    }

    if (!Objects.isNull(patch.urls())) {
      List<String> urlListToAdd =
          patch.urls().stream().filter(u -> !u.isBlank()).map(String::trim).toList();
      saveUrls(taskEntity, urlListToAdd);
    } else {
      logger.info("No URLs to add for task ID {}", taskId);
    }
  }
}
