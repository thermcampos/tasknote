package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.NoteUrl;
import br.com.tasknoteapp.server.entity.Tag;
import br.com.tasknoteapp.server.entity.TaskNoteTag;
import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.exception.InternalValidationException;
import br.com.tasknoteapp.server.exception.NoteArchivedException;
import br.com.tasknoteapp.server.exception.NoteNotArchivedException;
import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.exception.RequestValidationException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import br.com.tasknoteapp.server.util.ValidationUtil;
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
    Optional<InternalValidationException> createValidation = isNoteRequestValid(noteRequest);
    if (createValidation.isPresent()) {
      throw new RequestValidationException(
          createValidation.get().getErrorKey(), createValidation.get().getMessage());
    }

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
      getOrCreateTags(noteRequest.tags(), user, created.id());
    }

    if (!Objects.isNull(noteRequest.url()) && !noteRequest.url().isEmpty()) {
      saveUrl(created, noteRequest.url());
    }

    int deleted = tagRepository.deleteOrphanedTags(user.getId());
    logger.info("Deleted {} tags for user", deleted);

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
   * @param patch An instance of {@link NoteRequest} with the new content.
   * @return {@link NoteResponse} containing the updated note.
   */
  @Transactional
  public NoteResponse patchNote(Long noteId, NoteRequest patch) {
    Optional<InternalValidationException> patchValidation = isNoteRequestValid(patch);
    if (patchValidation.isPresent()) {
      throw new RequestValidationException(
          patchValidation.get().getErrorKey(), patchValidation.get().getMessage());
    }

    User user = getCurrentUser();

    logger.info("Patching note ID {} to user ID {}", noteId, user.getId());

    Optional<Note> note = noteRepository.findByIdAndUserId(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    Note noteEntity = note.get();
    if (noteEntity.archived()) {
      throw new NoteArchivedException();
    }

    var title = noteEntity.title();
    if (!Objects.isNull(patch.title()) && !patch.title().isBlank()) {
      title = patch.title().trim();
    }
    var description = noteEntity.description();
    if (!Objects.isNull(patch.description()) && !patch.description().isBlank()) {
      description = patch.description();
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

    logger.info("Note patched! ID {}", patchedNote.id());

    patchNoteUrl(patchedNote, patch, user);

    patchNoteTags(patchedNote, patch, user);

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
      throw new NoteNotArchivedException();
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
    logger.info("{} notes found!", notes.size());
    
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

    logger.info("Handling {} tags: {}", normalizedNames.size(), normalizedNames);

    Set<Tag> tags = new HashSet<>();
    for (String name : normalizedNames) {
      Optional<Tag> tagOp = tagRepository.findByUserIdAndName(user.getId(), name);

      if (tagOp.isEmpty()) {
        Tag newTag = tagRepository.save(new Tag(null, name, user.getId()), "notes", noteId);
        tags.add(newTag);
        logger.info("Saved new tag {} for note {}", name, noteId);
      } else {
        tagRepository.updateTagForNote(tagOp.get(), noteId);
        tags.add(tagOp.get());
        logger.info("Kept existing tag {} for note {}", name, noteId);
      }
    }

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

  private void patchNoteTags(Note note, NoteRequest patch, User user) {
    if (!Objects.isNull(patch.tags()) && !patch.tags().isEmpty()) {
      getOrCreateTags(patch.tags(), user, note.id());
    }

    List<TaskNoteTag> noteTags = tagRepository
        .findAllByUserIdAndNoteIdInList(user.getId(),  List.of(note.id()));

    logger.info("Found {} tags for note {}", noteTags.size(), note.id());

    List<TaskNoteTag> toDelete = new ArrayList<>();
    
    for (TaskNoteTag tnt : noteTags) {
      if (!Objects.isNull(patch.tags()) && !patch.tags().contains(tnt.name())) {
        logger.info("Flagged tag {} to be deleted for note {}", tnt, note.id());
        toDelete.add(tnt);
      }
    }

    List<Long> tagsIds = toDelete.stream().map((t) -> t.tagId()).toList();
    
    if (!tagsIds.isEmpty()) {
      int deleted = tagRepository.deleteTagFromNote(tagsIds, note.id());
      logger.info("Deleted {} tags from note id {}", deleted, note.id());
    }

    int deletedOrphan = tagRepository.deleteOrphanedTags(user.getId());
    logger.info("Deleted {} orphaned tags from note id {}", deletedOrphan, note.id());
  }

  private void patchNoteUrl(Note note, NoteRequest patch, User user) {
    if (!Objects.isNull(patch.url()) && !patch.url().isBlank()) {
      getOrCreateUrls(List.of(patch.url()), user, note.id());
    }

    List<NoteUrl> noteUrls = noteUrlRepository.findAllByNoteIdList(List.of(note.id()));

    logger.info("Found {} urls for note {}", noteUrls.size(), note.id());

    int deletedCount = 0;
    
    for (NoteUrl nu : noteUrls) {
      if (!Objects.isNull(patch.url()) && !patch.url().contains(nu.url())) {
        noteUrlRepository.deleteByNoteId(note.id());
        deletedCount++;
      }
    }

    if (deletedCount > 0) {
      logger.info("Deleted {} url from note id {}", deletedCount, note.id());
    }

    int deletedOrphan = tagRepository.deleteOrphanedTags(user.getId());
    logger.info("Deleted {} orphaned urls from note id {}", deletedOrphan, note.id());
  }

  private Set<NoteUrl> getOrCreateUrls(List<String> urls, User user, Long noteId) {
    if (Objects.isNull(urls) || urls.isEmpty()) {
      return new HashSet<>();
    }

    Set<String> normalizedUrls =
        urls.stream()
            .filter(name -> !Objects.isNull(name) && !name.isBlank())
            .map(name -> name.trim().toLowerCase())
            .collect(Collectors.toSet());

    logger.info("Handling {} urls: {}", normalizedUrls.size(), normalizedUrls);

    List<NoteUrl> currentNoteUrls = noteUrlRepository.findAllByNoteIdList(List.of(noteId));

    logger.info("Found {} urls: {} for note {}", currentNoteUrls.size(), currentNoteUrls, noteId);

    Set<NoteUrl> noteUrls = new HashSet<>();

    for (String url : normalizedUrls) {
      Optional<NoteUrl> noteUrlOp = currentNoteUrls
          .stream()
          .filter((tu) -> tu.url().equals(url))
          .findFirst();

      if (noteUrlOp.isEmpty()) {
        NoteUrl newNotekUrl = new NoteUrl(null, noteId, url);
        NoteUrl added = noteUrlRepository.save(newNotekUrl);
        noteUrls.add(added);
      } else {
        noteUrls.add(noteUrlOp.get());
      }
    }

    return noteUrls;
  }
  
  private List<NoteResponse> buildNoteResponse(List<Note> noteList, Long userId) {
    if (noteList.isEmpty()) {
      return List.of();
    }

    List<Long> allNotesIds = noteList.stream().map((t) -> t.id()).toList();
    List<TaskNoteTag> notesTags = tagRepository.findAllByUserIdAndNoteIdInList(userId, allNotesIds);
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

  private Optional<InternalValidationException> isNoteRequestValid(NoteRequest request) {
    try {
      // Title
      ValidationUtil.notNullNorBlank("title", request.title());
      ValidationUtil.maxSize("title", request.title(), ValidationUtil.MAX_NOTE_TITLE_SIZE);

      // Description
      ValidationUtil.notNullNorBlank("description", request.description());
      ValidationUtil.maxSize("description", request.description(),
          ValidationUtil.MAX_NOTE_CONTENT_SIZE);
      
      // URLs
      if (!Objects.isNull(request.url()) && !request.url().isBlank()) {
        ValidationUtil.maxSize("url", request.url(), ValidationUtil.MAX_URL_SIZE);
        ValidationUtil.url("url", request.url());
      }

      // Tags
      if (!Objects.isNull(request.tags()) && !request.tags().isEmpty()) {
        for (String tag : request.tags()) {
          int idx = request.tags().indexOf(tag);
          ValidationUtil.maxSize("tag" + idx, tag, ValidationUtil.MAX_TAG_NAME_SIZE);
        }
      }

      return Optional.empty();
    } catch (InternalValidationException ex) {
      return Optional.of(ex);
    }
  }
}
