package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.entity.TaskNoteTag;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    return buildTaskResponse(tasks, user.getId());
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

    logger.info("Task found! ID {}", taskId);

    List<TaskResponse> responseList = buildTaskResponse(List.of(task.get()), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during TaskResponse build in getTaskById");
    }

    return responseList.getFirst();
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

    LocalDate dueDate = null;
    if (!Objects.isNull(taskRequest.dueDate()) && !taskRequest.dueDate().isBlank()) {
      dueDate = LocalDate.parse(taskRequest.dueDate());
    }

    Task task = new Task(
          null,
          user.getId(),
          taskRequest.description(),
          Boolean.FALSE,
          LocalDateTime.now(),
          dueDate,
          Boolean.FALSE,
          Boolean.FALSE,
          taskRequest.highPriority()
    );
    
    Task created = taskRepository.save(task);

    logger.info("Task created! ID {}", created.id());

    if (!Objects.isNull(taskRequest.tags())) {
      getOrCreateTags(taskRequest.tags(), user, created.id());
    }

    if (!Objects.isNull(taskRequest.urls()) && !taskRequest.urls().isEmpty()) {
      saveUrls(created, taskRequest.urls());
    }

    int deleted = tagRepository.deleteOrphanedTags(user.getId());
    logger.info("Deleted {} tags for user", deleted);

    List<TaskResponse> responseList = buildTaskResponse(List.of(created), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during TaskResponse build in createTask");
    }

    return responseList.getFirst();
  }

  /**
   * Patch a task for the current user.
   *
   * @param taskId The task id from the database.
   * @param patchRequest An instance of {@link TaskPatchRequest} with the content to be patched.
   * @return {@link TaskResponse} with the updated content.
   */
  @Transactional
  public TaskResponse patchTask(Long taskId, TaskPatchRequest patchRequest) {
    User user = getCurrentUser();

    logger.info("Patching task ID {} to user ID {}", taskId, user.getId());

    Optional<Task> task = taskRepository.findByIdAndUserId(taskId, user.getId());
    if (task.isEmpty()) {
      throw new TaskNotFoundException();
    }

    Task taskEntity = task.get();

    var description = taskEntity.description();
    if (!Objects.isNull(patchRequest.description()) && !patchRequest.description().isBlank()) {
      description = patchRequest.description().trim();
    }
    var completed = taskEntity.completed();
    if (!Objects.isNull(patchRequest.completed())) {
      completed = patchRequest.completed();
    }

    var highPriority = taskEntity.highPriority();
    if (!Objects.isNull(patchRequest.highPriority())) {
      highPriority = patchRequest.highPriority();
    }

    Task taskToPatch = new Task(
        taskId,
        user.getId(),
        description,
        completed,
        LocalDateTime.now(),
        resolvDueDate(taskEntity, patchRequest),
        Boolean.FALSE,
        Boolean.FALSE,
        highPriority
    );

    Task patchedTask = taskRepository.save(taskToPatch);

    logger.info("Task patched! ID {}", taskId);

    patchTaskUrl(patchedTask, patchRequest);

    if (!Objects.isNull(patchRequest.tags()) && !patchRequest.tags().isEmpty()) {
      getOrCreateTags(patchRequest.tags(), user, taskId);
    }

    List<TaskNoteTag> taskTags = tagRepository.findAllByUserIdAndTaskIdInList(
        user.getId(),  List.of(taskId));
    List<TaskNoteTag> toDelete = new ArrayList<>();
    for (TaskNoteTag tnt : taskTags) {
      if (!patchRequest.tags().contains(tnt.name())) {
        toDelete.add(tnt);
      }
    }

    List<Long> tagsIds = toDelete.stream().map((t) -> t.tagId()).toList();
    if (!tagsIds.isEmpty()) {
      int deleted = tagRepository.deleteTagFromTask(tagsIds, taskId);
      logger.info("Deleted {} tags from task id {}", deleted, taskId);
    }

    int deletedOrphan = tagRepository.deleteOrphanedTags(user.getId());
    logger.info("Deleted {} orphaned tags from task id {}", deletedOrphan, taskId);

    List<TaskResponse> responseList = buildTaskResponse(List.of(patchedTask), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during TaskResponse build in patchTask");
    }

    return responseList.getFirst();
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

    List<TaskUrl> urlsToDelete = taskUrlRepository.findAllById_taskId(taskEntity.id());
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

    return buildTaskResponse(tasks, user.getId());
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
            .filter(t -> t.completed().equals(Boolean.FALSE))
            .toList();
    
    List<TaskResponse> responseList = buildTaskResponse(allTasks, user.getId());

    return switch (filter) {
      case "all" -> responseList;
      case "high" ->
          responseList
              .stream()
              .filter(((tr) -> tr.highPriority().equals(Boolean.TRUE)))
              .toList();
      case "untagged" ->
          responseList
              .stream()
              .filter((tr) -> tr.tags().isEmpty())
              .toList();
      default ->
          responseList
              .stream()
              .filter((tr) -> tr.tags().stream().anyMatch((tag) -> tag.equals(filter)))
              .toList();
    };
  }

  private List<TaskResponse> buildTaskResponse(List<Task> taskList, Long userId) {
    if (taskList.isEmpty()) {
      return List.of();
    }

    List<Long> allTaskIds = taskList.stream().map((t) -> t.id()).toList();
    List<TaskNoteTag> taskTags = tagRepository.findAllByUserIdAndTaskIdInList(userId, allTaskIds);
    
    Map<Long, List<Tag>> tagMap = new HashMap<>();
    for (TaskNoteTag taskTag : taskTags) {
      tagMap.putIfAbsent(taskTag.taskNoteId(), new ArrayList<>());
      tagMap.get(taskTag.taskNoteId())
          .add(new Tag(taskTag.tagId(), taskTag.name(), taskTag.userId()));
    }

    List<TaskResponse> responseList = new ArrayList<>();
    for (Task t : taskList) {
      List<Tag> tagsFromMap = tagMap.getOrDefault(t.id(), new ArrayList<>());
      TaskResponse tr = TaskResponse.fromEntity(t, getAllTasksUrls(t.id()), tagsFromMap);
      responseList.add(tr);
    }

    return responseList;
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

    logger.info("Handling {} tags: {}", normalizedNames.size(), normalizedNames);

    Set<Tag> tags = new HashSet<>();
    for (String name : normalizedNames) {
      Optional<Tag> tagOp = tagRepository.findByUserIdAndName(user.getId(), name);

      if (tagOp.isEmpty()) {
        Tag newTag = tagRepository.save(new Tag(null, name, user.getId()), "tasks", taskId);
        tags.add(newTag);
      } else {
        tagRepository.updateTagForTask(tagOp.get(), taskId);
        tags.add(tagOp.get());
      }
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
    return urls.stream().map((tu) -> tu.id()).map((pk) -> pk.url()).toList();
  }

  private void saveUrls(Task taskEntity, List<String> urls) {
    List<TaskUrl> tasksUrl = new ArrayList<>();
    for (String url : urls) {
      TaskUrlPk pk = new TaskUrlPk(taskEntity.id(), url);
      TaskUrl taskUrl = new TaskUrl(pk);
      tasksUrl.add(taskUrl);
    }

    taskUrlRepository.saveAll(tasksUrl);
    logger.info("Added {} URLs from task ID {}", tasksUrl.size(), taskEntity.id());
  }

  private LocalDate resolvDueDate(Task taskEntity, TaskPatchRequest patch) {
    LocalDate dueDate = null;
    if (!Objects.isNull(patch.dueDate()) && !patch.dueDate().isBlank()) {
      try {
        dueDate = LocalDate.parse(patch.dueDate());
      } catch (DateTimeParseException e) {
        logger.error("Unable to parse the provided date: {}: {}", patch.dueDate(), e.getMessage());
      }
    }
    return dueDate;
  }

  private void patchTaskUrl(Task taskEntity, TaskPatchRequest patch) {
    Long taskId = taskEntity.id();
    List<TaskUrl> urlsToDelete = taskUrlRepository.findAllById_taskId(taskId);
    if (!urlsToDelete.isEmpty()) {
      taskUrlRepository.deleteAllById_taskId(taskId);
      logger.info("Deleted {} URLs from task ID {}", urlsToDelete.size(), taskId);
    } else {
      logger.info("No URLs to delete for task ID {}", taskId);
    }

    if (!Objects.isNull(patch.urls())) {
      List<String> urlListToAdd =
          patch.urls().stream().filter(u -> !u.isBlank()).map((u) -> u.trim()).toList();
      saveUrls(taskEntity, urlListToAdd);
    } else {
      logger.info("No URLs to add for task ID {}", taskId);
    }
  }
}
