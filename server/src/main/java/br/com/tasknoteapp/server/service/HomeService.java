package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** This class contains the implementation for the Home Service class. */
@Service
public class HomeService {

  private static final Logger logger = LoggerFactory.getLogger(HomeService.class);

  private final TaskService taskService;

  private final NoteService noteService;

  private final TagRepository tagRepository;

  private final AuthService authService;

  private final AuthUtil authUtil;

  /**
   * Constructor for the HomeService class.
   *
   * @param taskService The service for task operations.
   * @param noteService The service for note operations.
   * @param tagRepository The repository for tag entities.
   * @param authService The service for authentication.
   * @param authUtil Utility class for authentication-related operations.
   */
  public HomeService(
      TaskService taskService,
      NoteService noteService,
      TagRepository tagRepository,
      AuthService authService,
      AuthUtil authUtil) {
    this.taskService = taskService;
    this.noteService = noteService;
    this.tagRepository = tagRepository;
    this.authService = authService;
    this.authUtil = authUtil;
  }

  /**
   * Get all existing tags, ordered alphabetically.
   *
   * @return List of String with the tags.
   */
  public List<String> getTopTasksTag() {
    User user = getCurrentUser();
    logger.info("Getting all tags for user ID {}", user.getId());

    List<String> tags =
        tagRepository.findAllByUserIdOrderByNameAsc(user.getId()).stream()
            .map((t) -> t.name())
            .toList();

    List<TaskResponse> tasks = taskService.getTasksByFilter("all");
    List<NoteResponse> notes = noteService.getAllNotes();

    boolean hasUntagged =
        tasks.stream().anyMatch(task -> task.tags().isEmpty())
            || notes.stream().anyMatch(note -> note.tags().isEmpty());

    if (hasUntagged) {
      tags = new java.util.ArrayList<>(tags);
      tags.add("untagged");
      tags = tags.stream().sorted().toList();
    }

    logger.info("Found {} tags", tags.size());

    return tags;
  }

  private User getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    return authService.findByEmail(email).orElseThrow();
  }
}
