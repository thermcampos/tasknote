package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import br.com.tasknoteapp.server.util.TimeAgoUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock TaskRepository taskRepository;

  @Mock AuthService authService;

  @Mock AuthUtil authUtil;

  @Mock TaskUrlRepository taskUrlRepository;

  @Mock TagRepository tagRepository;

  private static final Long USER_ID = 123L;

  private static final String USER_EMAIL = "test@domain.com";

  private TaskService taskService;

  @BeforeEach
  void setup() {
    taskService =
        new TaskService(
            taskRepository, authService, authUtil, taskUrlRepository, tagRepository);
  }

  @Test
  @DisplayName("Get tasks by id happy path should succeed")
  void getTaskById_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 9976L;

    Task taskEntity = new Task(
        taskId,
        userEntity.getId(),
        "Test task",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );
    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    Tag tagDev = new Tag(1L, "test", USER_ID);
    TaskNoteTag taskTags = new TaskNoteTag(tagDev.id(), tagDev.name(), tagDev.userId(),
        taskEntity.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(taskEntity.id())))
            .thenReturn(List.of(taskTags));

    TaskResponse taskResponse = taskService.getTaskById(taskId);

    assertNotNull(taskResponse);
    assertEquals(taskEntity.id(), taskResponse.id());
    assertEquals(taskEntity.description(), taskResponse.description());
    assertEquals(taskEntity.highPriority(), taskResponse.highPriority());
    assertTrue(taskResponse.tags().contains("test"));
  }

  @Test
  @DisplayName("Get tasks by id not found should fail")
  void getTaskById_notFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 9976L;

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(taskId));
  }

  @Test
  @DisplayName("Create a task null dueDate should succeed")
  void createTask_nullDueDate_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request = new TaskRequest("Write unit tests", null, null, false, tags);

    Task entity = new Task(
        123L,
        null,
        request.description(),
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        request.highPriority()
    );
    when(taskRepository.save(any())).thenReturn(entity);

    Tag tag = new Tag(333L, "development", USER_ID);
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), eq(tag.name())))
        .thenReturn(Optional.of(tag));

    when(tagRepository.deleteOrphanedTags(USER_ID)).thenReturn(0);

    TaskNoteTag taskTags = new TaskNoteTag(tag.id(), tag.name(), tag.userId(), entity.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(entity.id())))
            .thenReturn(List.of(taskTags));

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertNotNull(response.id());
    assertTrue(response.tags().stream().anyMatch(t -> t.equals("development")));
  }

  @ParameterizedTest
  @CsvSource({"'', null", "'2025-12-12', '2025-12-12'"})
  @DisplayName("Create a task with various dueDate values should succeed")
  void createTask_parametrizedDueDate_shouldSucceed(String dueDate, String expectedDueDate) {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request = new TaskRequest("Write unit tests", null, dueDate, false, tags);

    Tag tagEntity = new Tag(null, "development", userEntity.getId());
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), anyString()))
        .thenReturn(Optional.of(tagEntity));

    Task entity = new Task(
        null,
        null,
        request.description(),
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        request.highPriority()
    );
    when(taskRepository.save(any())).thenReturn(entity);

    taskService.createTask(request);

    assertNotNull(entity);
    // FIXME get tags
    // assertTrue(entity.getTags().stream().anyMatch(t -> t.getName().equals("development")));
  }

  @Test
  @DisplayName("Create task with null url it should succeed")
  void createTask_nullUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest("Write unit tests", null, "2025-12-12", false, tags);

    Tag tagEntity = new Tag(null, "development", userEntity.getId());
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), anyString()))
        .thenReturn(Optional.of(tagEntity));

    Task entity = new Task(
        123L,
        null,
        request.description(),
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        request.highPriority()
    );
    when(taskRepository.save(any())).thenReturn(entity);

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertTrue(response.urls().isEmpty());
  }

  @Test
  @DisplayName("Create task with empty url it should succeed")
  void createTask_emptyUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest("Write unit tests", List.of(), "2025-12-12", false, tags);

    Tag tagEntity = new Tag(null, "development", userEntity.getId());
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), anyString()))
        .thenReturn(Optional.of(tagEntity));

    Task entity = new Task(
        123L,
        null,
        request.description(),
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        request.highPriority()
    );
    when(taskRepository.save(any())).thenReturn(entity);

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertTrue(response.urls().isEmpty());
  }

  @Test
  @DisplayName("Create task with valid url it should succeed")
  void createTask_fullUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest(
            "Write unit tests", List.of("debian.org"), "2025-12-12", false, tags);

    Tag tagEntity = new Tag(null, "development", userEntity.getId());
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), anyString()))
        .thenReturn(Optional.of(tagEntity));

    Task entity = new Task(
        123L,
        null,
        request.description(),
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        request.highPriority()
    );

    when(taskRepository.save(any())).thenReturn(entity);

    TaskUrl urlEntity = new TaskUrl(new TaskUrlPk(entity.id(), "debian.org"));
    when(taskUrlRepository.findAllById_taskId(entity.id())).thenReturn(List.of(urlEntity));

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertFalse(response.urls().isEmpty());
    assertEquals("debian.org", response.urls().get(0));
  }

  @Test
  @DisplayName("Get all tasks happy path should succeed")
  void getAllTasks_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task entity = new Task(
        123L,
        null,
        "Writ unit tests",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of(entity));

    Tag tagDev = new Tag(1L, "dev", USER_ID);
    TaskNoteTag taskTags = new TaskNoteTag(tagDev.id(), tagDev.name(), tagDev.userId(),
        entity.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(entity.id())))
            .thenReturn(List.of(taskTags));

    List<TaskResponse> responses = taskService.getAllTasks();

    assertFalse(responses.isEmpty());
    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("dev"));
  }

  @Test
  @DisplayName("Delete a task following the happy path should succeed")
  void deleteTask_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    Task taskEntity = new Task(
        taskId,
        userEntity.getId(),
        "Test task",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    when(taskRepository.delete(taskEntity)).thenReturn(1);

    taskService.deleteTask(taskId);

    verify(taskRepository, times(1)).delete(any());
  }

  @Test
  @DisplayName("Delete a not existing tasks it should fail")
  void deleteTask_notFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2526L;

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(taskId));
  }

  @Test
  @DisplayName("Patch a task following the happy path it should succeed")
  void patchTask_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    Task taskEntity = new Task(
        taskId,
        userEntity.getId(),
        "Test task",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    final String dueDate = "2026-12-31";

    Task savedTask = new Task(
        taskId,
        userEntity.getId(),
        "Test task updated",
        Boolean.TRUE,
        null,
        LocalDate.parse(dueDate),
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    when(taskRepository.save(any())).thenReturn(savedTask);

    Tag tag = new Tag(22L, "test", userEntity.getId());
    TaskNoteTag taskTags = new TaskNoteTag(tag.id(), tag.name(), tag.userId(),
        savedTask.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(savedTask.id())))
            .thenReturn(List.of(taskTags));

    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest(true, "Test task updated", null, dueDate, false, tags);
    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.completed());
    assertEquals(TimeAgoUtil.formatDueDate(LocalDate.parse(dueDate)), patched.dueDateFmt());
    assertFalse(patched.highPriority());
    assertTrue(patched.tags().contains("test"));
    assertTrue(patched.urls().isEmpty());
  }

  @Test
  @DisplayName("Patch a task with url it should succeed")
  void patchTask_withUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    Task taskEntity = new Task(
        taskId,
        userEntity.getId(),
        "Test task",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    TaskUrl urlEntity = new TaskUrl(new TaskUrlPk(taskId, "www.url.com"));
    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of(urlEntity));
    //when(taskUrlRepository.deleteAllById_taskId(taskId)).thenReturn(1);

    final String dueDate = "2026-12-31";

    Tag tag = new Tag(510L, "test", userEntity.getId());
    when(tagRepository.findByUserIdAndName(eq(userEntity.getId()), anyString()))
        .thenReturn(Optional.of(tag));

    Task savedTask = new Task(
        taskId,
        userEntity.getId(),
        "Test task updated",
        Boolean.TRUE,
        null,
        LocalDate.parse(dueDate),
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    when(taskRepository.save(any())).thenReturn(savedTask);

    String url = "http://test.com";
    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest(true, "Test task updated", List.of(url), dueDate, false, tags);

    when(taskUrlRepository.saveAll(any())).thenReturn(0);

    TaskNoteTag taskTags = new TaskNoteTag(tag.id(), tag.name(), tag.userId(),
        savedTask.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(savedTask.id())))
            .thenReturn(List.of(taskTags));

    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.completed());
    assertEquals(TimeAgoUtil.formatDueDate(LocalDate.parse(dueDate)), patched.dueDateFmt());
    assertFalse(patched.highPriority());
    assertTrue(patched.tags().contains("test"));
    assertFalse(patched.urls().isEmpty());
  }

  @Test
  @DisplayName("Patch a task with a not found task should fail")
  void patchTask_taskNotFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.empty());

    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest(true, "Test task updated", null, "2025-12-31", false, tags);

    assertThrows(TaskNotFoundException.class, () -> taskService.patchTask(taskId, patch));
  }

  @Test
  @DisplayName("Patch a task with a due date parse exception should fail")
  void patchTask_dueDateParseException_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    Task taskEntity = new Task(
        taskId,
        userEntity.getId(),
        "Test task",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findByIdAndUserId(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    Task savedTask = new Task(
        taskId,
        userEntity.getId(),
        "Test task updated",
        Boolean.TRUE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    when(taskRepository.save(any())).thenReturn(savedTask);

    // wrong due date
    String dueDate = "2026-31-31";

    Tag tag = new Tag(616L, "test", userEntity.getId());
    List<String> tags = List.of(tag.name());
    TaskNoteTag taskTags = new TaskNoteTag(tag.id(), tag.name(), tag.userId(),
        savedTask.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(savedTask.id())))
        .thenReturn(List.of(taskTags));

    TaskPatchRequest patch =
        new TaskPatchRequest(true, "Test task updated", null, dueDate, false, tags);
    
    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.completed());
    assertNull(patched.dueDate());
    assertNull(patched.dueDateFmt());
    assertFalse(patched.highPriority());
    assertTrue(patched.tags().contains("test"));
    assertTrue(patched.urls().isEmpty());
  }

  @Test
  @DisplayName("Search tasks with matching term should succeed")
  void searchTasks_matchingTerm_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task taskEntity = new Task(
        1L,
        userEntity.getId(),
        "Write unit tests",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    String searchTerm = "unit";
    when(taskRepository.findAllBySearchTerm(searchTerm.toUpperCase(), USER_ID))
        .thenReturn(List.of(taskEntity));

    List<TaskResponse> responses = taskService.searchTasks(searchTerm);

    assertNotNull(responses);
    assertFalse(responses.isEmpty());
    assertEquals(1, responses.size());
    assertEquals(taskEntity.description(), responses.get(0).description());
  }

  @Test
  @DisplayName("Search tasks with no matching term should return empty list")
  void searchTasks_noMatchingTerm_shouldReturnEmptyList() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    String searchTerm = "nonexistent";

    when(taskRepository.findAllBySearchTerm(searchTerm.toUpperCase(), USER_ID))
        .thenReturn(List.of());

    List<TaskResponse> responses = taskService.searchTasks(searchTerm);

    assertNotNull(responses);
    assertTrue(responses.isEmpty());
  }

  @Test
  @DisplayName("Search tasks with null search term should return empty list")
  void searchTasks_nullSearchTerm_shouldReturnEmptyList() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    String searchTerm = null;

    List<TaskResponse> responses = taskService.searchTasks(searchTerm);

    assertNotNull(responses);
    assertTrue(responses.isEmpty());
  }

  @Test
  @DisplayName("Get tasks by filter 'all' should succeed")
  void getTasksByFilter_all_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task task1 = new Task(
        1L,
        userEntity.getId(),
        "Task 1",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    Task task2 = new Task(
        2L,
        userEntity.getId(),
        "Task 2",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of(task1, task2));

    TaskNoteTag tag1 = new TaskNoteTag(null, "tag1", USER_ID, 1L);
    TaskNoteTag tag2 = new TaskNoteTag(null, "tag2", USER_ID, 2L);
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(1L, 2L)))
        .thenReturn(List.of(tag1, tag2));

    List<TaskResponse> responses = taskService.getTasksByFilter("all");

    assertEquals(2, responses.size());
    assertTrue(responses.get(0).tags().contains("tag1"));
    assertTrue(responses.get(1).tags().contains("tag2"));
  }

  @Test
  @DisplayName("Get tasks by filter 'high' should succeed")
  void getTasksByFilter_high_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task task1 = new Task(
        1L,
        userEntity.getId(),
        "Task 1",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    Task task2 = new Task(
        2L,
        userEntity.getId(),
        "Task 2",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of(task1, task2));
    TaskNoteTag tag1 = new TaskNoteTag(22L, "tag2", USER_ID, 2L);
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(1L, 2L)))
        .thenReturn(List.of(tag1));

    List<TaskResponse> responses = taskService.getTasksByFilter("high");

    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("tag2"));
  }

  @Test
  @DisplayName("Get tasks by filter 'untagged' should succeed")
  void getTasksByFilter_untagged_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task task1 = new Task(
        1L,
        userEntity.getId(),
        "Task 1",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    Task task2 = new Task(
        2L,
        userEntity.getId(),
        "Task 2",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of(task1, task2));

    List<TaskResponse> responses = taskService.getTasksByFilter("untagged");

    assertEquals(2, responses.size());
    assertTrue(responses.get(0).tags().isEmpty());
    assertTrue(responses.get(1).tags().isEmpty());
  }

  @Test
  @DisplayName("Get tasks by specific tag filter should succeed")
  void getTasksByFilter_specificTag_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Task task1 = new Task(
        858L,
        userEntity.getId(),
        "Task 1",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );

    Task task2 = new Task(
        870L,
        userEntity.getId(),
        "Task 2",
        Boolean.FALSE,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE
    );

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of(task1, task2));

    Tag tag1 = new Tag(883L, "tag1", USER_ID);
    TaskNoteTag taskTags = new TaskNoteTag(tag1.id(), tag1.name(), tag1.userId(), task2.id());
    when(tagRepository.findAllByUserIdAndTaskIdInList(USER_ID, List.of(task1.id(), task2.id())))
        .thenReturn(List.of(taskTags));

    List<TaskResponse> responses = taskService.getTasksByFilter("tag1");

    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("tag1"));
  }

  @Test
  @DisplayName("Get tasks by filter with no matching tasks should return empty list")
  void getTasksByFilter_noMatchingTasks_shouldReturnEmptyList() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    User userEntity = new User();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    when(taskRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

    List<TaskResponse> responses = taskService.getTasksByFilter("nonexistent");

    assertTrue(responses.isEmpty());
  }
}
