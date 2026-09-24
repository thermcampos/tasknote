package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.response.HomeItemsResponse;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** This class contains the implementation for the Home Service class. */
@Service
public class HomeService {

  private static final Logger logger = LoggerFactory.getLogger(HomeService.class);

  private static final String UNTAGGED = "untagged";

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

    Set<String> tagSet = new HashSet<>(tagRepository.findAllTagNamesByUserId(user.getId()));

    if (tagRepository.userHasUntaggedItems(user.getId())) {
      tagSet.add(UNTAGGED);
    }

    List<String> tags = tagSet
        .stream()
        .sorted()
        .toList();

    logger.info("Found {} tags", tags.size());

    return tags;
  }

  /**
   * Get the Home items payload. Without search or tag filters it returns the default window
   * (items touched in the last 24h plus high-priority incomplete tasks); with filters it runs an
   * unbounded query composing all params with AND semantics.
   *
   * @param searchTerm Optional text to search by.
   * @param tag Optional tag name to filter by ("untagged" matches items without tags).
   * @param type Optional item type: all, tasks or notes.
   * @return {@link HomeItemsResponse} with the tasks and notes found.
   */
  public HomeItemsResponse getHomeItems(String searchTerm, String tag, String type) {
    User user = getCurrentUser();
    logger.info(
        "Getting home items for user ID {}, q: {}, tag: {}, type: {}",
        user.getId(), searchTerm, tag, type);

    boolean hasSearch = searchTerm != null && !searchTerm.isBlank();
    boolean hasTag = tag != null && !tag.isBlank();
    boolean windowed = !hasSearch && !hasTag;

    boolean includeTasks = type == null || type.isBlank() || "all".equals(type)
        || "tasks".equals(type);
    boolean includeNotes = type == null || type.isBlank() || "all".equals(type)
        || "notes".equals(type);

    List<TaskResponse> tasks =
        includeTasks
            ? taskService.getHomeTasks(searchTerm, tag, windowed)
            : List.of();
    List<NoteResponse> notes =
        includeNotes
            ? noteService.getHomeNotes(searchTerm, tag, windowed)
            : List.of();

    return new HomeItemsResponse(tasks, notes);
  }

  private User getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    return authService.findByEmail(email).orElseThrow();
  }
}
