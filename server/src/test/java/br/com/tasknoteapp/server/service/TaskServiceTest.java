package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.TagEntity;
import br.com.tasknoteapp.server.entity.TaskEntity;
import br.com.tasknoteapp.server.entity.TaskUrlEntity;
import br.com.tasknoteapp.server.entity.TaskUrlEntityPk;
import br.com.tasknoteapp.server.entity.UserEntity;
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
import java.util.Set;
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

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 9976L;

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(taskId);
    taskEntity.setDescription("Test task");
    taskEntity.setHighPriority(true);
    taskEntity.setTags(Set.of(new TagEntity("test", userEntity)));
    taskEntity.setUser(userEntity);
    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    TaskResponse taskResponse = taskService.getTaskById(taskId);

    assertNotNull(taskResponse);
    assertEquals(taskEntity.getId(), taskResponse.id());
    assertEquals(taskEntity.getDescription(), taskResponse.description());
    assertEquals(taskEntity.getHighPriority(), taskResponse.highPriority());
    assertTrue(taskResponse.tags().contains("test"));
  }

  @Test
  @DisplayName("Get tasks by id not found should fail")
  void getTaskById_notFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 9976L;

    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(taskId));
  }

  @Test
  @DisplayName("Create a task null dueDate should succeed")
  void createTask_nullDueDate_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request = new TaskRequest("Write unit tests", null, null, false, tags);

    TagEntity tagEntity = new TagEntity("development", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity entity = new TaskEntity();
    entity.setDescription(request.description());
    entity.setHighPriority(request.highPriority());
    entity.setTags(Set.of(tagEntity));
    when(taskRepository.save(any())).thenReturn(entity);

    taskService.createTask(request);

    assertNotNull(entity);
    assertTrue(entity.getTags().stream().anyMatch(t -> t.getName().equals("development")));
  }

  @ParameterizedTest
  @CsvSource({"'', null", "'2025-12-12', '2025-12-12'"})
  @DisplayName("Create a task with various dueDate values should succeed")
  void createTask_parametrizedDueDate_shouldSucceed(String dueDate, String expectedDueDate) {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request = new TaskRequest("Write unit tests", null, dueDate, false, tags);

    TagEntity tagEntity = new TagEntity("development", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity entity = new TaskEntity();
    entity.setDescription(request.description());
    entity.setHighPriority(request.highPriority());
    entity.setTags(Set.of(tagEntity));
    when(taskRepository.save(any())).thenReturn(entity);

    taskService.createTask(request);

    assertNotNull(entity);
    assertTrue(entity.getTags().stream().anyMatch(t -> t.getName().equals("development")));
  }

  @Test
  @DisplayName("Create task with null url it should succeed")
  void createTask_nullUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest("Write unit tests", null, "2025-12-12", false, tags);

    TagEntity tagEntity = new TagEntity("development", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity entity = new TaskEntity();
    entity.setId(123L);
    entity.setDescription(request.description());
    entity.setHighPriority(request.highPriority());
    entity.setTags(Set.of(tagEntity));
    when(taskRepository.save(any())).thenReturn(entity);

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertTrue(response.urls().isEmpty());
  }

  @Test
  @DisplayName("Create task with empty url it should succeed")
  void createTask_emptyUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest("Write unit tests", List.of(), "2025-12-12", false, tags);

    TagEntity tagEntity = new TagEntity("development", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity entity = new TaskEntity();
    entity.setId(123L);
    entity.setDescription(request.description());
    entity.setHighPriority(request.highPriority());
    entity.setTags(Set.of(tagEntity));
    when(taskRepository.save(any())).thenReturn(entity);

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertTrue(response.urls().isEmpty());
  }

  @Test
  @DisplayName("Create task with valid url it should succeed")
  void createTask_fullUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    List<String> tags = List.of("development");
    TaskRequest request =
        new TaskRequest(
            "Write unit tests", List.of("debian.org"), "2025-12-12", false, tags);

    TagEntity tagEntity = new TagEntity("development", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity entity = new TaskEntity();
    entity.setId(123L);
    entity.setDescription(request.description());
    entity.setHighPriority(request.highPriority());
    entity.setTags(Set.of(tagEntity));
    when(taskRepository.save(any())).thenReturn(entity);

    TaskUrlEntity urlEntity = new TaskUrlEntity();
    urlEntity.setId(new TaskUrlEntityPk(entity.getId(), "debian.org"));
    when(taskUrlRepository.findAllById_taskId(entity.getId())).thenReturn(List.of(urlEntity));

    TaskResponse response = taskService.createTask(request);

    assertNotNull(response);
    assertFalse(response.urls().isEmpty());
    assertEquals("debian.org", response.urls().get(0));
  }

  @Test
  @DisplayName("Get all tasks happy path should succeed")
  void getAllTasks_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity entity = new TaskEntity();
    entity.setDescription("Writ unit tests");
    entity.setHighPriority(true);
    entity.setTags(Set.of(new TagEntity("dev", userEntity)));
    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of(entity));

    List<TaskResponse> responses = taskService.getAllTasks();

    assertFalse(responses.isEmpty());
    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("dev"));
  }

  @Test
  @DisplayName("Delete a task following the happy path should succeed")
  void deleteTask_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(taskId);
    taskEntity.setDescription("Test task");
    taskEntity.setHighPriority(true);
    taskEntity.setTags(Set.of(new TagEntity("test", userEntity)));
    taskEntity.setUser(userEntity);
    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    doNothing().when(taskRepository).delete(taskEntity);

    taskService.deleteTask(taskId);

    verify(taskRepository, times(1)).delete(any());
  }

  @Test
  @DisplayName("Delete a not existing tasks it should fail")
  void deleteTask_notFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2526L;

    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.empty());

    assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(taskId));
  }

  @Test
  @DisplayName("Patch a task following the happy path it should succeed")
  void patchTask_happyPath_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(taskId);
    taskEntity.setDescription("Test task");
    taskEntity.setHighPriority(true);
    taskEntity.setDone(false);
    taskEntity.setTags(Set.of(new TagEntity("test", userEntity)));
    taskEntity.setUser(userEntity);
    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    final String dueDate = "2026-12-31";

    TagEntity tagEntity = new TagEntity("test", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity savedTask = new TaskEntity();
    savedTask.setDescription("Test task updated");
    savedTask.setHighPriority(false);
    savedTask.setDueDate(LocalDate.parse(dueDate));
    savedTask.setDone(true);
    savedTask.setTags(taskEntity.getTags());
    when(taskRepository.save(any())).thenReturn(savedTask);

    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest("Test task updated", true, null, dueDate, false, tags);
    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.done());
    assertEquals(TimeAgoUtil.formatDueDate(LocalDate.parse(dueDate)), patched.dueDateFmt());
    assertFalse(patched.highPriority());
    assertTrue(patched.tags().contains("test"));
    assertTrue(patched.urls().isEmpty());
  }

  @Test
  @DisplayName("Patch a task with url it should succeed")
  void patchTask_withUrl_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(taskId);
    taskEntity.setDescription("Test task");
    taskEntity.setHighPriority(true);
    taskEntity.setDone(false);
    taskEntity.setTags(Set.of(new TagEntity("test", userEntity)));
    taskEntity.setUser(userEntity);
    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    TaskUrlEntity urlEntity = new TaskUrlEntity();
    urlEntity.setId(new TaskUrlEntityPk(taskId, "www.url.com"));
    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of(urlEntity));
    doNothing().when(taskUrlRepository).deleteAllById_taskId(taskId);

    final String dueDate = "2026-12-31";

    TagEntity tagEntity = new TagEntity("test", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity savedTask = new TaskEntity();
    savedTask.setDescription("Test task updated");
    savedTask.setHighPriority(false);
    savedTask.setDueDate(LocalDate.parse(dueDate));
    savedTask.setDone(true);
    savedTask.setTags(taskEntity.getTags());
    when(taskRepository.save(any())).thenReturn(savedTask);

    String url = "http://test.com";
    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest("Test task updated", true, List.of(url), dueDate, false, tags);

    when(taskUrlRepository.saveAll(any())).thenReturn(List.of());
    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.done());
    assertEquals(TimeAgoUtil.formatDueDate(LocalDate.parse(dueDate)), patched.dueDateFmt());
    assertFalse(patched.highPriority());
    assertTrue(patched.tags().contains("test"));
    assertFalse(patched.urls().isEmpty());
  }

  @Test
  @DisplayName("Patch a task with a not found task should fail")
  void patchTask_taskNotFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.empty());

    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest("Test task updated", true, null, "2025-12-31", false, tags);

    assertThrows(TaskNotFoundException.class, () -> taskService.patchTask(taskId, patch));
  }

  @Test
  @DisplayName("Patch a task with a due date parse exception should fail")
  void patchTask_dueDateParseException_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    Long taskId = 2525L;

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(taskId);
    taskEntity.setDescription("Test task");
    taskEntity.setHighPriority(true);
    taskEntity.setDone(false);
    taskEntity.setTags(Set.of(new TagEntity("test", userEntity)));
    taskEntity.setUser(userEntity);
    when(taskRepository.findByIdAndUser_id(taskId, USER_ID)).thenReturn(Optional.of(taskEntity));

    when(taskUrlRepository.findAllById_taskId(taskId)).thenReturn(List.of());

    TagEntity tagEntity = new TagEntity("test", userEntity);
    when(tagRepository.findByNameAndUser_id(anyString(), anyLong()))
        .thenReturn(Optional.of(tagEntity));

    TaskEntity savedTask = new TaskEntity();
    savedTask.setDescription("Test task updated");
    savedTask.setHighPriority(false);
    savedTask.setDone(true);
    savedTask.setTags(taskEntity.getTags());
    when(taskRepository.save(any())).thenReturn(savedTask);

    // wrong due date
    String dueDate = "2026-31-31";

    List<String> tags = List.of("test");
    TaskPatchRequest patch =
        new TaskPatchRequest("Test task updated", true, null, dueDate, false, tags);
    TaskResponse patched = taskService.patchTask(taskId, patch);

    assertNotNull(patched);
    assertEquals("Test task updated", patched.description());
    assertTrue(patched.done());
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

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setId(1L);
    taskEntity.setDescription("Write unit tests");
    taskEntity.setHighPriority(false);
    taskEntity.setTags(Set.of(new TagEntity("development", userEntity)));

    String searchTerm = "unit";
    when(taskRepository.findAllBySearchTerm(searchTerm.toUpperCase(), USER_ID))
        .thenReturn(List.of(taskEntity));

    List<TaskResponse> responses = taskService.searchTasks(searchTerm);

    assertNotNull(responses);
    assertFalse(responses.isEmpty());
    assertEquals(1, responses.size());
    assertEquals(taskEntity.getDescription(), responses.get(0).description());
  }

  @Test
  @DisplayName("Search tasks with no matching term should return empty list")
  void searchTasks_noMatchingTerm_shouldReturnEmptyList() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
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

    UserEntity userEntity = new UserEntity();
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

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity task1 = new TaskEntity();
    task1.setId(1L);
    task1.setDescription("Task 1");
    task1.setHighPriority(false);
    task1.setDone(false);
    task1.setTags(Set.of(new TagEntity("tag1", userEntity)));

    TaskEntity task2 = new TaskEntity();
    task2.setId(2L);
    task2.setDescription("Task 2");
    task2.setHighPriority(true);
    task2.setDone(false);
    task2.setTags(Set.of(new TagEntity("tag2", userEntity)));

    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of(task1, task2));

    List<TaskResponse> responses = taskService.getTasksByFilter("all");

    assertEquals(2, responses.size());
    assertTrue(responses.get(0).tags().contains("tag1"));
    assertTrue(responses.get(1).tags().contains("tag2"));
  }

  @Test
  @DisplayName("Get tasks by filter 'high' should succeed")
  void getTasksByFilter_high_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity task1 = new TaskEntity();
    task1.setId(1L);
    task1.setDescription("Task 1");
    task1.setHighPriority(false);
    task1.setDone(false);
    task1.setTags(Set.of(new TagEntity("tag1", userEntity)));

    TaskEntity task2 = new TaskEntity();
    task2.setId(2L);
    task2.setDescription("Task 2");
    task2.setHighPriority(true);
    task2.setDone(false);
    task2.setTags(Set.of(new TagEntity("tag2", userEntity)));

    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of(task1, task2));

    List<TaskResponse> responses = taskService.getTasksByFilter("high");

    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("tag2"));
  }

  @Test
  @DisplayName("Get tasks by filter 'untagged' should succeed")
  void getTasksByFilter_untagged_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity task1 = new TaskEntity();
    task1.setId(1L);
    task1.setDescription("Task 1");
    task1.setHighPriority(false);
    task1.setDone(false);
    task1.setTags(Set.of());

    TaskEntity task2 = new TaskEntity();
    task2.setId(2L);
    task2.setDescription("Task 2");
    task2.setHighPriority(true);
    task2.setDone(false);
    task2.setTags(Set.of());

    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of(task1, task2));

    List<TaskResponse> responses = taskService.getTasksByFilter("untagged");

    assertEquals(2, responses.size());
    assertTrue(responses.get(0).tags().isEmpty());
    assertTrue(responses.get(1).tags().isEmpty());
  }

  @Test
  @DisplayName("Get tasks by specific tag filter should succeed")
  void getTasksByFilter_specificTag_shouldSucceed() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    TaskEntity task1 = new TaskEntity();
    task1.setId(1L);
    task1.setDescription("Task 1");
    task1.setHighPriority(false);
    task1.setDone(false);
    task1.setTags(Set.of(new TagEntity("tag1", userEntity)));

    TaskEntity task2 = new TaskEntity();
    task2.setId(2L);
    task2.setDescription("Task 2");
    task2.setHighPriority(true);
    task2.setDone(false);
    task2.setTags(Set.of(new TagEntity("tag2", userEntity)));

    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of(task1, task2));

    List<TaskResponse> responses = taskService.getTasksByFilter("tag1");

    assertEquals(1, responses.size());
    assertTrue(responses.get(0).tags().contains("tag1"));
  }

  @Test
  @DisplayName("Get tasks by filter with no matching tasks should return empty list")
  void getTasksByFilter_noMatchingTasks_shouldReturnEmptyList() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(USER_EMAIL));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(USER_ID);
    userEntity.setEmail(USER_EMAIL);
    when(authService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(userEntity));

    when(taskRepository.findAllByUser_id(USER_ID)).thenReturn(List.of());

    List<TaskResponse> responses = taskService.getTasksByFilter("nonexistent");

    assertTrue(responses.isEmpty());
  }
}
