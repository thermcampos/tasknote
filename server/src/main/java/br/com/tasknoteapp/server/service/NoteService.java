package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.NoteUrl;
import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.TaskNoteTag;
import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.exception.NoteArchivedException;
import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.request.NotePatchRequest;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** This class implements the NoteService interface methods. */
@Service
public class NoteService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final NoteRepository noteRepository;

  private final AuthService authService;

  private final AuthUtil authUtil;

  private final NoteUrlRepository noteUrlRepository;

  private final TagRepository tagRepository;

  /**
   * Constructor for the NoteService class.
   *
   * @param noteRepository The repository for note entities.
   * @param authService The service for authentication.
   * @param authUtil Utility class for authentication-related operations.
   * @param noteUrlRepository The repository for note URL entities.
   * @param tagRepository The repository for tag entities.
   */
  public NoteService(
      NoteRepository noteRepository,
      AuthService authService,
      AuthUtil authUtil,
      NoteUrlRepository noteUrlRepository,
      TagRepository tagRepository) {
    this.noteRepository = noteRepository;
    this.authService = authService;
    this.authUtil = authUtil;
    this.noteUrlRepository = noteUrlRepository;
    this.tagRepository = tagRepository;
  }

  /**
   * Get all notes for the current user.
   *
   * @return {@link List} of {@link NoteResponse} with all notes or an empty list.
   */
  @Transactional 
  public List<NoteResponse> getAllNotes() {
    User user = getCurrentUser();

    logger.info("Get all notes to user ID {}", user.getId());

    List<Note> notes = noteRepository.findAllByUserId(user.getId());
    logger.info("{} notes found!", notes.size());

    return buildNoteResponse(notes, user.getId());
  }

  /**
   * Get a note by its id.
   *
   * @param noteId The note id in the database.
   * @return {@link NoteResponse} with the found note or throw a {@link NoteNotFoundException}.
   */
  @Transactional
  public NoteResponse getNoteById(Long noteId) {
    User user = getCurrentUser();
    logger.info("Get note ID {} to user ID {}", noteId, user.getId());

    Optional<Note> note = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    logger.info("Note found! ID {}", noteId);

    List<NoteResponse> responseList = buildNoteResponse(List.of(note.get()), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in getNoteById");
    }

    return responseList.getFirst();
  }

  /**
   * Create a note for the user.
   *
   * @param noteRequest The note content.
   * @return {@link NoteResponse} with created note data.
   */
  @Transactional
  public NoteResponse createNote(NoteRequest noteRequest) {
    User user = getCurrentUser();

    logger.info("Creating note to user ID {}", user.getId());

    Note note = new Note(
        null,
        user.getId(),
        noteRequest.description(),
        noteRequest.title(),
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.FALSE
    );
    Note created = noteRepository.save(note);

    logger.info("Note created! ID {}", created.id());

    if (!Objects.isNull(noteRequest.tags())) {
      Set<Tag> tagSet = getOrCreateTags(noteRequest.tags(), user, created.id());
      logger.info("Get or create tags for note id {}, set returned {} items",
          created.id(), tagSet.size());
    }

    if (!Objects.isNull(noteRequest.url()) && !noteRequest.url().isEmpty()) {
      saveUrl(created, noteRequest.url());
    }

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Finished note creation!");

    List<NoteResponse> responseList = buildNoteResponse(List.of(created), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in createNote");
    }

    return responseList.getFirst();
  }

  /**
   * Patch an existing note updating its content.
   *
   * @param noteId The note id from the database.
   * @param patch An instance of {@link NotePatchRequest} with the new content.
   * @return {@link NoteResponse} containing the updated note.
   */
  @Transactional
  public NoteResponse patchNote(Long noteId, NotePatchRequest patch) {
    User user = getCurrentUser();

    logger.info("Patching task ID {} to user ID {}", noteId, user.getId());

    Optional<Note> note = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = note.get();
    if (noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    String title = noteEntity.title();
    if (!Objects.isNull(patch.title()) && !patch.title().isBlank()) {
      title = patch.title().trim();
    }
    String description = noteEntity.description();
    if (!Objects.isNull(patch.description()) && !patch.description().isBlank()) {
      description = patch.description();
    }
    if (!Objects.isNull(patch.tags())) {
      Set<Tag> tagSet = getOrCreateTags(patch.tags(), user, noteId);
      logger.info("Get or create tags for note id {}, set returned {} items",
          noteId, tagSet.size());
    }

    noteUrlRepository.deleteByNoteId(noteId);
    logger.info("URL deleted from note ID {} during patch", noteId);

    if (!Objects.isNull(patch.url()) && !patch.url().isBlank()) {
      saveUrl(noteEntity, patch.url());
    } else {
      logger.info("No URLs to patch for note ID {}", noteId);
    }

    Note noteToPatch = new Note(
        noteId,
        user.getId(),
        description,
        title,
        LocalDateTime.now(),
        noteEntity.shared(),
        noteEntity.shareToken(),
        Boolean.FALSE
    );

    Note patchedNote = noteRepository.save(noteToPatch);

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Note patched! ID {}", patchedNote.id());

    List<NoteResponse> responseList = buildNoteResponse(List.of(patchedNote), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in patchNote");
    }

    return responseList.getFirst();
  }

  /**
   * Delete all notes and their URLs for the current user, regardless of archived state.
   *
   * <p>This is intended for account deletion, where the trash-can rule (only archived notes may be
   * deleted) does not apply.
   */
  @Transactional
  public void deleteAllNotesForCurrentUser() {
    User user = getCurrentUser();

    logger.info("Deleting all notes for user ID {}", user.getId());

    List<Note> notes = noteRepository.findAllByUserId(user.getId());
    for (Note note : notes) {
      noteUrlRepository.deleteByNoteId(note.id());
      noteRepository.delete(note);
    }

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("All {} notes deleted for user ID {}", notes.size(), user.getId());
  }

  /**
   * Delete a note and all its URLs, if any, for the user.
   *
   * @param noteId The note id from the database.
   */
  @Transactional
  public void deleteNote(Long noteId) {
    User user = getCurrentUser();

    logger.info("Deleting note ID {} to user ID {}", noteId, user.getId());

    Optional<Note> note = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = note.get();
    if (!noteEntity.archived()) {
      // FIXME create a not archived exception
      throw new NoteArchivedException();
    }

    noteUrlRepository.deleteByNoteId(noteId);
    logger.info("URL deleted from note ID {}", noteId);

    noteRepository.delete(noteEntity);

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Note deleted! ID {}", noteId);
  }

  /**
   * Search for notes given a search term.
   *
   * @param searchTerm The term to be used for the search.
   * @return {@link List} of {@link NoteResponse} with found records or an empty list.
   */
  @Transactional
  public List<NoteResponse> searchNotes(String searchTerm) {
    User user = getCurrentUser();

    logger.info("Searching notes for user ID {}", user.getId());

    List<Note> notes =
        noteRepository.findAllBySearchTerm(user.getId(), searchTerm.toUpperCase());
    logger.info("{} tasks found!", notes.size());
    
    return buildNoteResponse(notes, user.getId());
  }

  /**
   * Share a note publicly, generating a unique share token.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the updated note with share token.
   */
  @Transactional
  public NoteResponse shareNote(Long noteId) {
    User user = getCurrentUser();
    logger.info("Sharing note ID {} for user ID {}", noteId, user.getId());

    Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = noteOpt.get();

    if (noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    if (Boolean.TRUE.equals(noteEntity.shared())) {
      // throw new NoteAlreadySharedException
      throw new RuntimeException("Note " + noteId + " already shared!");
    }

    Note noteToShare = new Note(
        noteId,
        user.getId(),
        noteEntity.description(),
        noteEntity.title(),
        LocalDateTime.now(),
        Boolean.TRUE,
        UUID.randomUUID().toString(),
        Boolean.FALSE
    );
    Note sharedNote = noteRepository.save(noteToShare);
    logger.info("Note ID {} shared with token {}", noteId, noteToShare.shareToken());

    List<NoteResponse> responseList = buildNoteResponse(List.of(sharedNote), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in shareNote");
    }

    return responseList.getFirst();
  }

  /**
   * Unshare a note, revoking public access.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the updated note.
   */
  @Transactional
  public NoteResponse unshareNote(Long noteId) {
    User user = getCurrentUser();
    logger.info("Unsharing note ID {} for user ID {}", noteId, user.getId());

    Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = noteOpt.get();

    if (noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    Note noteToUnShare = new Note(
        noteId,
        user.getId(),
        noteEntity.description(),
        noteEntity.title(),
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.FALSE
    );

    Note unsharedNote = noteRepository.save(noteToUnShare);
    logger.info("Note ID {} unshared", noteId);

    List<NoteResponse> responseList = buildNoteResponse(List.of(unsharedNote), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in unshareNote");
    }

    return responseList.getFirst();
  }

  /**
   * Get a publicly shared note by its share token (no authentication required).
   *
   * @param shareToken The unique share token for the note.
   * @return {@link NoteResponse} containing the shared note.
   */
  @Transactional
  public NoteResponse getSharedNote(String shareToken) {
    logger.info("Fetching shared note with token {}", shareToken);

    Optional<Note> noteOpt = noteRepository.findByShareToken(shareToken);
    if (noteOpt.isEmpty() || !noteOpt.get().shared() || noteOpt.get().archived()) {
      throw new NoteNotFoundException();
    }

    List<NoteResponse> responseList = buildNoteResponse(List.of(noteOpt.get()),
        noteOpt.get().userId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in shareNote");
    }

    return responseList.getFirst();
  }

  private Set<Tag> getOrCreateTags(List<String> tagNames, User user, Long noteId) {
    if (Objects.isNull(tagNames) || tagNames.isEmpty()) {
      return new HashSet<>();
    }

    Set<String> normalizedNames =
        tagNames.stream()
            .filter(name -> !Objects.isNull(name) && !name.isBlank())
            .map(name -> name.trim().toLowerCase())
            .collect(Collectors.toSet());

    logger.info("getOrCreateTags - tags {}", tagNames);
    Set<Tag> tags = new HashSet<>();
    for (String name : normalizedNames) {
      Tag tag = tagRepository
              .findByUserIdAndName(user.getId(), name)
              .orElseGet(() -> tagRepository.save(
                new Tag(null, name, user.getId()), "notes", noteId));
      tags.add(tag);
    }
    logger.info("getOrCreateTags - tags completed {}", tags);
    return tags;
  }

  private User getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    return authService.findByEmail(email).orElseThrow();
  }

  private String getNoteUrl(Long noteId) {
    Optional<NoteUrl> noteUrl = noteUrlRepository.findByNoteId(noteId);
    return noteUrl.isPresent() ? noteUrl.get().url() : null;
  }

  // private List<NoteResponse> getNotesUrl(List<Note> notes) {
  //   List<Long> noteIds = notes.stream().map((n) -> n.id()).toList();
  //   if (noteIds.isEmpty()) {
  //     // TODO: review empty tag list
  //     return notes.stream().map(n -> NoteResponse.fromEntity(n, null, List.of())).toList();
  //   }
  //   List<NoteUrl> urls = noteUrlRepository.findAllByNoteIdList(noteIds);
  //   Map<Long, String> noteUrls = new HashMap<>();
  //   for (NoteUrl nu : urls) {
  //     noteUrls.put(nu.noteId(), nu.url());
  //   }

  //   // TODO: review empty tag list
  //   return notes
  //       .stream()
  //       .map(n -> NoteResponse.fromEntity(n, noteUrls.get(n.id()), List.of()))
  //       .toList();
  // }

  /**
   * Archive a note, disabling edits and revoking public sharing.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the archived note.
   */
  @Transactional
  public NoteResponse archiveNote(Long noteId) {
    User user = getCurrentUser();
    logger.info("Archiving note ID {} for user ID {}", noteId, user.getId());

    Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = noteOpt.get();
    if (noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    Note noteToArchive = new Note(
        noteId,
        user.getId(),
        noteEntity.description(),
        noteEntity.title(),
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.TRUE
    );

    Note archivedNote = noteRepository.save(noteToArchive);
    logger.info("Note ID {} archived", noteId);

    List<NoteResponse> responseList = buildNoteResponse(List.of(archivedNote), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in archiveNote");
    }

    return responseList.getFirst();
  }

  /**
   * Restore an archived note back to active state.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the restored note.
   */
  @Transactional
  public NoteResponse restoreNote(Long noteId) {
    User user = getCurrentUser();
    logger.info("Restoring note ID {} for user ID {}", noteId, user.getId());

    Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = noteOpt.get();
    if (!noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    Note noteToRestore = new Note(
        noteId,
        user.getId(),
        noteEntity.description(),
        noteEntity.title(),
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.FALSE
    );

    Note restoredNote = noteRepository.save(noteToRestore);
    logger.info("Note ID {} restored", noteId);

    List<NoteResponse> responseList = buildNoteResponse(List.of(restoredNote), user.getId());
    if (responseList.isEmpty()) {
      throw new RuntimeException("Issues during NoteResponse build in restoreNote");
    }

    return responseList.getFirst();
  }

  private List<NoteResponse> buildNoteResponse(List<Note> noteList, Long userId) {
    if (noteList.isEmpty()) {
      return List.of();
    }

    List<Long> allNotesIds = noteList.stream().map((t) -> t.id()).toList();
    List<TaskNoteTag> notesTags = tagRepository.findAllByUserIdAndTaskIdInList(userId, allNotesIds);
    Map<Long, List<Tag>> tagMap = new HashMap<>();
    for (TaskNoteTag noteTag : notesTags) {
      tagMap.putIfAbsent(noteTag.taskNoteId(), new ArrayList<>());
      tagMap.get(noteTag.taskNoteId())
          .add(new Tag(noteTag.tagId(), noteTag.name(), noteTag.userId()));
    }

    List<NoteResponse> responseList = new ArrayList<>();
    for (Note n : noteList) {
      List<Tag> tagsFromMap = tagMap.getOrDefault(n.id(), new ArrayList<>());
      NoteResponse tr = NoteResponse.fromEntity(n, getNoteUrl(n.id()), tagsFromMap);
      responseList.add(tr);
    }

    return responseList;
  }

  private NoteUrl saveUrl(Note noteEntity, String url) {
    NoteUrl noteUrl = new NoteUrl(null, noteEntity.id(), url);
    NoteUrl savedUrl = noteUrlRepository.save(noteUrl);
    logger.info("URL saved to note ID {}", noteEntity.id());
    return savedUrl;
  }
}
