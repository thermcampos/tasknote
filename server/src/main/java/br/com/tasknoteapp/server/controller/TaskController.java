package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.exception.TaskNotFoundException;
import br.com.tasknoteapp.server.request.TaskPatchRequest;
import br.com.tasknoteapp.server.request.TaskRequest;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.service.TaskService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This class contains resources for handling tasks. */
@RestController
@RequestMapping("/rest/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  /**
   * Get all tasks.
   *
   * @return List of TaskResponse with all found tasks and its URLs, if any.
   */
  @GetMapping
  public List<TaskResponse> getAllTasks() {
    return taskService.getAllTasks();
  }

  /**
   * Get a task by its ID.
   *
   * @param id Task identification.
   * @return TaskResponse with task data and its URLs, if any.
   * @throws TaskNotFoundException when task not found.
   */
  @GetMapping("/{id}")
  public TaskResponse getTaskById(@PathVariable Long id) {
    return taskService.getTaskById(id);
  }

  /**
   * Patch a task.
   *
   * @param id The task id to be patched.
   * @param taskRequest Task data to be patched, including optionally its URLs.
   * @return TaskResponse containing data that was updated.
   * @throws TaskNotFoundException when task not found.
   */
  @PatchMapping("/{id}")
  public ResponseEntity<TaskResponse> patchTask(
      @PathVariable Long id, @RequestBody TaskPatchRequest taskRequest) {
    return ResponseEntity.ok(taskService.patchTask(id, taskRequest));
  }

  /**
   * Create a task.
   *
   * @param taskRequest Task data to be created, including optionally its URLs. Following RESTful
   *     API pattern from <a href="https://restfulapi.net/rest-put-vs-post/">docs</a>.
   * @return TaskResponse containing data that was created.
   */
  @PostMapping
  public ResponseEntity<TaskResponse> postTasks(@RequestBody TaskRequest taskRequest) {
    TaskResponse response = taskService.createTask(taskRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Delete a task given its ID.
   *
   * @param id Task identification.
   * @throws TaskNotFoundException when task not found.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
