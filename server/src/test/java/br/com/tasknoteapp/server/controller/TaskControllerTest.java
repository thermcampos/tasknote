package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.exception.TaskNotFoundException;
import br.com.tasknoteapp.server.request.TaskPatchRequest;
import br.com.tasknoteapp.server.request.TaskRequest;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.service.TaskService;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @Test
  @DisplayName("Get all tasks with some tasks found should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getAllTasks_tasksFound_shouldSucceed() throws Exception {
    TaskResponse taskResponse =
        new TaskResponse(
            1L, false, "Desc", true, null, null, "Moments ago", List.of("tag"), List.of("http://test.com"));
    when(taskService.getAllTasks()).thenReturn(List.of(taskResponse));

    mockMvc
        .perform(
            get("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(taskResponse.id()))
        .andExpect(jsonPath("$[0].description").value(taskResponse.description()))
        .andExpect(jsonPath("$[0].completed", Matchers.is(false)))
        .andExpect(jsonPath("$[0].highPriority", Matchers.is(true)))
        .andExpect(jsonPath("$[0].dueDate", Matchers.nullValue()))
        .andExpect(jsonPath("$[0].dueDateFmt", Matchers.nullValue()))
        .andExpect(jsonPath("$[0].lastUpdate").value("Moments ago"))
        .andExpect(jsonPath("$[0].urls[0]").value(taskResponse.urls().get(0)))
        .andReturn();
  }

  @Test
  @DisplayName("Get all tasks with no tasks found should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getAllTasks_noTasksFound_shouldSucceed() throws Exception {
    when(taskService.getAllTasks()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", Matchers.empty()))
        .andReturn();
  }

  @Test
  @DisplayName("Get all tasks with 401 unauthorized request should fail")
  void getAllTasks_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            get("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Get task by id happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getTaskById_happyPath_shouldSucceed() throws Exception {
    Long taskId = 999L;
    TaskResponse taskResponse =
        new TaskResponse(
            taskId,
            false,
            "Desc",
            true,
            null,
            null,
            "Moments ago",
            List.of("tag"),
            List.of("http://test.com"));
    when(taskService.getTaskById(taskId)).thenReturn(taskResponse);

    mockMvc
        .perform(
            get("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskResponse.id()))
        .andExpect(jsonPath("$.description").value(taskResponse.description()))
        .andExpect(jsonPath("$.completed", Matchers.is(false)))
        .andExpect(jsonPath("$.highPriority", Matchers.is(true)))
        .andExpect(jsonPath("$.dueDate", Matchers.nullValue()))
        .andExpect(jsonPath("$.dueDateFmt", Matchers.nullValue()))
        .andExpect(jsonPath("$.lastUpdate").value("Moments ago"))
        .andExpect(jsonPath("$.urls[0]").value(taskResponse.urls().get(0)))
        .andReturn();
  }

  @Test
  @DisplayName("Get task by id not found should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getTaskById_notFound_shouldFail() throws Exception {
    Long taskId = 998L;
    when(taskService.getTaskById(taskId)).thenThrow(new TaskNotFoundException());

    mockMvc
        .perform(
            get("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andReturn();
  }

  @Test
  @DisplayName("Get task by id unauthorized should fail")
  void getTaskById_unauthorized_shouldFail() throws Exception {
    Long taskId = 997L;

    mockMvc
        .perform(
            get("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Patch a task via patch request happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void patchTask_happyPath_shouldSucceed() throws Exception {
    Long taskId = 111L;
    TaskPatchRequest patchRequest =
        new TaskPatchRequest(false, "Description patched", List.of(), null, true, List.of("tag"));

    TaskResponse taskResponse =
        new TaskResponse(
            taskId,
            false,
            "Description patched",
            true,
            null,
            null,
            "Moments ago",
            List.of("tag"),
            List.of());
    when(taskService.patchTask(taskId, patchRequest)).thenReturn(taskResponse);

    final String payloadJson =
        """
        {
          "description": "Description patched",
          "completed": false,
          "urls": [],
          "highPriority": true
        }
        """;

    mockMvc
        .perform(
            patch("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  @DisplayName("Patch a task via patch request with not found id should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void patchTask_notFound_shouldFail() throws Exception {
    Long taskId = 118L;
    TaskPatchRequest patchRequest =
        new TaskPatchRequest(false, "Description patched", List.of(), null, true, List.of("tag"));

    when(taskService.patchTask(taskId, patchRequest)).thenThrow(new TaskNotFoundException());

    final String payloadJson =
        """
        {
          "description": "Description patched",
          "completed": false,
          "urls": [],
          "highPriority": true,
          "tags": ["tag"]
        }
        """;

    mockMvc
        .perform(
            patch("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isNotFound())
        .andReturn();
  }

  @Test
  @DisplayName("Patch a task via patch request with 401 unauthorized exception")
  void patchTask_unauthorized_shouldFail() throws Exception {
    Long taskId = 111L;

    final String payloadJson =
        """
        {
          "description": "Description patched",
          "completed": false,
          "urls": [],
          "highPriority": true
        }
        """;

    mockMvc
        .perform(
            patch("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Post create task happy path should succeed and return 201")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void postTasks_happyPath_shouldSucceed() throws Exception {
    TaskRequest request =
        new TaskRequest("Test task", List.of("www.url.com"), null, true, List.of("tag"));

    TaskResponse taskResponse =
        new TaskResponse(
            858L,
            false,
            "Description patched",
            true,
            null,
            null,
            "Moments ago",
            List.of("tag"),
            List.of());
    when(taskService.createTask(request)).thenReturn(taskResponse);

    final String payloadJson =
        """
        {
          "description": "Test task",
          "urls": ["https://www.url.com"],
          "highPriority": true,
          "tags": ["tag"]
        }
        """;

    mockMvc
        .perform(
            post("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isCreated())
        .andReturn();
  }

  @Test
  @DisplayName("Post create task with missing information should fail with 400 bad request")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void postTasks_missingInformation_shouldFail() throws Exception {
    final String payloadJson =
        """
        {
          "description": ""
        }
        """;

    mockMvc
        .perform(
            post("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @DisplayName("Post create task with 401 unauthorized request should fail")
  void postTasks_unauthorized_shouldFail() throws Exception {
    final String payloadJson =
        """
        {
          "description": "Any description here"
        }
        """;

    mockMvc
        .perform(
            post("/rest/tasks")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Delete task request happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteTask_happyPath_shouldSucceed() throws Exception {
    final Long taskId = 333L;

    doNothing().when(taskService).deleteTask(taskId);

    mockMvc
        .perform(
            delete("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("Delete task with 401 unauthorized request should fail")
  void deleteTask_unauthorized_shouldFail() throws Exception {
    final Long taskId = 533L;

    mockMvc
        .perform(
            delete("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Delete task with 404 request not found should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteTask_notFound_shouldFail() throws Exception {
    final Long taskId = 433L;

    doThrow(new TaskNotFoundException()).when(taskService).deleteTask(taskId);

    mockMvc
        .perform(
            delete("/rest/tasks/{id}", taskId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andReturn();
  }
}
