package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.exception.UserNotFoundException;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.response.JwtAuthenticationResponse;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.response.UserResponse;
import br.com.tasknoteapp.server.util.SecurityUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** This class contains methods to handle user session and account deletion. */
@Service
public class UserSessionService {

  private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class.getName());

  private final AuthService authService;

  private final TaskService taskService;

  private final NoteService noteService;

  private final TagRepository tagRepository;

  /**
   * Constructor for UserSessionService.
   *
   * @param authService the authentication service
   * @param taskService the task service
   * @param noteService the note service
   * @param tagRepository the tag repository
   */
  public UserSessionService(
      AuthService authService,
      TaskService taskService,
      NoteService noteService,
      TagRepository tagRepository) {
    this.authService = authService;
    this.taskService = taskService;
    this.noteService = noteService;
    this.tagRepository = tagRepository;
  }

  /**
   * Refresh the current user session with a new JWT token.
   *
   * @return The new JWT token generated
   */
  public JwtAuthenticationResponse refreshUserSession() {
    String token = authService.refreshCurrentUserToken();
    return new JwtAuthenticationResponse(token);
  }

  /**
   * Delete the current user account content and data.
   *
   * <p>Each downstream deletion runs in its own transaction. Callers must verify the password
   * (via {@link AuthService#verifyCurrentPassword}) before invoking this method, so that a wrong
   * password does not open a transaction whose rollback would wipe the recorded attempt.
   *
   * @return {@link UserResponse} with user data
   */
  public UserResponse deleteCurrentUserAccount() {
    Optional<UserEntity> userOptional = authService.getCurrentUser();
    if (userOptional.isEmpty()) {
      throw new UserNotFoundException();
    }

    logger.info(
        "Delete current user account for user ID {} email {}",
        userOptional.get().getId(),
        SecurityUtil.redactEmail(userOptional.get().getEmail()));

    List<TaskResponse> tasks = taskService.getAllTasks();
    for (TaskResponse task : tasks) {
      Long taskId = task != null ? task.id() : null;
      if (taskId != null) {
        taskService.deleteTask(taskId);
      }
    }

    noteService.deleteAllNotesForCurrentUser();

    tagRepository.deleteAllForUser(userOptional.get().getId());

    UserResponse response = authService.deleteUserAccount();
    logger.info("User account deleted for user ID {}", userOptional.get().getId());
    return response;
  }
}
