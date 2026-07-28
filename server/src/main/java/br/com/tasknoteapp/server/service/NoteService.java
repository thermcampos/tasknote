package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.NoteEntity;
import br.com.tasknoteapp.server.entity.NoteUrlEntity;
import br.com.tasknoteapp.server.entity.TagEntity;
import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.exception.NoteArchivedException;
import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.request.NotePatchRequest;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
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

/** This class implements the NoteService interface methods. */
@Service
public class NoteService {

  private static final Logger logger = LoggerFactory.getLogger(NoteService.class);

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
    UserEntity user = getCurrentUser();

    logger.info("Get all notes to user ID {}", user.getId());

    List<NoteEntity> notes = noteRepository.findAllByUser_id(user.getId());
    logger.info("{} notes found!", notes.size());

    return getNotesUrl(notes);
  }

  /**
   * Get a note by its id.
   *
   * @param noteId The note id in the database.
   * @return {@link NoteResponse} with the found note or throw a {@link NoteNotFoundException}.
   */
  @Transactional
  public NoteResponse getNoteById(Long noteId) {
    UserEntity user = getCurrentUser();
    logger.info("Get note ID {} to user ID {}", noteId, user.getId());

    Optional<NoteEntity> note = noteRepository.findById(noteId);
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    if (!note.get().getUser().getId().equals(user.getId())) {
      throw new NoteNotFoundException();
    }

    logger.info("Note found! ID {}", noteId);
    return NoteResponse.fromEntity(note.get(), getNoteUrl(noteId));
  }

  /**
   * Create a note for the user.
   *
   * @param noteRequest The note content.
   * @return {@link NoteResponse} with created note data.
   */
  @Transactional
  public NoteResponse createNote(NoteRequest noteRequest) {
    UserEntity user = getCurrentUser();

    logger.info("Creating note to user ID {}", user.getId());

    NoteEntity note = new NoteEntity();
    note.setTitle(noteRequest.title());
    note.setDescription(noteRequest.description());
    if (!Objects.isNull(noteRequest.tags())) {
      note.setTags(getOrCreateTags(noteRequest.tags(), user));
    }
    note.setLastUpdate(LocalDateTime.now());
    note.setUser(user);
    NoteEntity created = noteRepository.save(note);

    logger.info("Note created! ID {}", created.getId());

    String savedUrl = null;
    if (!Objects.isNull(noteRequest.url()) && !noteRequest.url().isEmpty()) {
      NoteUrlEntity urlEntity = saveUrl(created, noteRequest.url());
      savedUrl = urlEntity.getUrl();
    }

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Finished note creation!");
    return NoteResponse.fromEntity(created, savedUrl);
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
    UserEntity user = getCurrentUser();

    logger.info("Patching task ID {} to user ID {}", noteId, user.getId());

    Optional<NoteEntity> note = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = note.get();
    if (noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    if (!Objects.isNull(patch.title()) && !patch.title().isBlank()) {
      noteEntity.setTitle(patch.title().trim());
    }
    if (!Objects.isNull(patch.description()) && !patch.description().isBlank()) {
      noteEntity.setDescription(patch.description());
    }
    if (!Objects.isNull(patch.tags())) {
      noteEntity.setTags(getOrCreateTags(patch.tags(), user));
    }
    noteEntity.setLastUpdate(LocalDateTime.now());

    noteUrlRepository.deleteByNote_id(noteId);
    noteUrlRepository.flush();
    logger.info("URL deleted from note ID {} during patch", noteId);

    if (!Objects.isNull(patch.url()) && !patch.url().isBlank()) {
      saveUrl(noteEntity, patch.url());
    } else {
      logger.info("No URLs to patch for note ID {}", noteId);
    }

    NoteEntity patchedNote = noteRepository.save(noteEntity);
    noteRepository.flush();

    tagRepository.deleteOrphanedTags(user.getId());

    logger.info("Note patched! ID {}", patchedNote.getId());

    return NoteResponse.fromEntity(patchedNote, getNoteUrl(patchedNote.getId()));
  }

  /**
   * Delete a note and all its URLs, if any, for the user.
   *
   * @param noteId The note id from the database.
   */
  @Transactional
  public void deleteNote(Long noteId) {
    UserEntity user = getCurrentUser();

    logger.info("Deleting note ID {} to user ID {}", noteId, user.getId());

    Optional<NoteEntity> note = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (note.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = note.get();
    if (!noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    noteUrlRepository.deleteByNote_id(noteId);
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
    UserEntity user = getCurrentUser();

    logger.info("Searching notes for user ID {}", user.getId());

    List<NoteEntity> notes =
        noteRepository.findAllBySearchTerm(searchTerm.toUpperCase(), user.getId());
    logger.info("{} tasks found!", notes.size());
    return getNotesUrl(notes);
  }

  /**
   * Share a note publicly, generating a unique share token.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the updated note with share token.
   */
  @Transactional
  public NoteResponse shareNote(Long noteId) {
    UserEntity user = getCurrentUser();
    logger.info("Sharing note ID {} for user ID {}", noteId, user.getId());

    Optional<NoteEntity> noteOpt = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = noteOpt.get();

    if (noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    if (!noteEntity.isShared()) {
      noteEntity.setShared(true);
      noteEntity.setShareToken(UUID.randomUUID().toString());
      noteRepository.save(noteEntity);
      logger.info("Note ID {} shared with token {}", noteId, noteEntity.getShareToken());
    }

    return NoteResponse.fromEntity(noteEntity, getNoteUrl(noteEntity.getId()));
  }

  /**
   * Unshare a note, revoking public access.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the updated note.
   */
  @Transactional
  public NoteResponse unshareNote(Long noteId) {
    UserEntity user = getCurrentUser();
    logger.info("Unsharing note ID {} for user ID {}", noteId, user.getId());

    Optional<NoteEntity> noteOpt = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = noteOpt.get();

    if (noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    noteEntity.setShared(false);
    noteEntity.setShareToken(null);
    noteRepository.save(noteEntity);
    logger.info("Note ID {} unshared", noteId);

    return NoteResponse.fromEntity(noteEntity, getNoteUrl(noteEntity.getId()));
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

    Optional<NoteEntity> noteOpt = noteRepository.findByShareToken(shareToken);
    if (noteOpt.isEmpty() || !noteOpt.get().isShared() || noteOpt.get().isArchived()) {
      throw new NoteNotFoundException();
    }

    return NoteResponse.fromEntity(noteOpt.get(), getNoteUrl(noteOpt.get().getId()));
  }

  private Set<TagEntity> getOrCreateTags(List<String> tagNames, UserEntity user) {
    if (Objects.isNull(tagNames) || tagNames.isEmpty()) {
      return new HashSet<>();
    }

    Set<String> normalizedNames =
        tagNames.stream()
            .filter(name -> !Objects.isNull(name) && !name.isBlank())
            .map(name -> name.trim().toLowerCase())
            .collect(Collectors.toSet());

    Set<TagEntity> tags = new HashSet<>();
    for (String name : normalizedNames) {
      TagEntity tag =
          tagRepository
              .findByNameAndUser_id(name, user.getId())
              .orElseGet(() -> tagRepository.save(new TagEntity(name, user)));
      tags.add(tag);
    }
    return tags;
  }

  private UserEntity getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    return authService.findByEmail(email).orElseThrow();
  }

  private String getNoteUrl(Long noteId) {
    return noteUrlRepository.findByNote_id(noteId).map(NoteUrlEntity::getUrl).orElse(null);
  }

  private List<NoteResponse> getNotesUrl(List<NoteEntity> notes) {
    List<Long> noteIds = notes.stream().map(NoteEntity::getId).toList();
    if (noteIds.isEmpty()) {
      return notes.stream().map(n -> NoteResponse.fromEntity(n, null)).toList();
    }
    List<NoteUrlEntity> urls = noteUrlRepository.findAllByNote_idIn(noteIds);
    Map<Long, String> noteUrls =
        urls.stream().collect(Collectors.toMap(nu -> nu.getNote().getId(), NoteUrlEntity::getUrl));

    return notes.stream().map(n -> NoteResponse.fromEntity(n, noteUrls.get(n.getId()))).toList();
  }

  /**
   * Archive a note, disabling edits and revoking public sharing.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the archived note.
   */
  @Transactional
  public NoteResponse archiveNote(Long noteId) {
    UserEntity user = getCurrentUser();
    logger.info("Archiving note ID {} for user ID {}", noteId, user.getId());

    Optional<NoteEntity> noteOpt = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = noteOpt.get();
    if (noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    noteEntity.setArchived(true);
    noteEntity.setShared(false);
    noteEntity.setShareToken(null);
    noteEntity.setLastUpdate(LocalDateTime.now());
    noteRepository.save(noteEntity);
    logger.info("Note ID {} archived", noteId);

    return NoteResponse.fromEntity(noteEntity, getNoteUrl(noteEntity.getId()));
  }

  /**
   * Restore an archived note back to active state.
   *
   * @param noteId The note id from the database.
   * @return {@link NoteResponse} containing the restored note.
   */
  @Transactional
  public NoteResponse restoreNote(Long noteId) {
    UserEntity user = getCurrentUser();
    logger.info("Restoring note ID {} for user ID {}", noteId, user.getId());

    Optional<NoteEntity> noteOpt = noteRepository.findByIdAndUser_id(noteId, user.getId());
    if (noteOpt.isEmpty()) {
      throw new NoteNotFoundException();
    }

    NoteEntity noteEntity = noteOpt.get();
    if (!noteEntity.isArchived()) {
      throw new NoteArchivedException();
    }

    noteEntity.setArchived(false);
    noteEntity.setLastUpdate(LocalDateTime.now());
    noteRepository.save(noteEntity);
    logger.info("Note ID {} restored", noteId);

    return NoteResponse.fromEntity(noteEntity, getNoteUrl(noteEntity.getId()));
  }

  private NoteUrlEntity saveUrl(NoteEntity noteEntity, String url) {
    NoteUrlEntity noteUrl = new NoteUrlEntity();
    noteUrl.setUrl(url);
    noteUrl.setNote(noteEntity);

    NoteUrlEntity savedUrl = noteUrlRepository.save(noteUrl);
    logger.info("URL saved to note ID {}", noteEntity.getId());

    return savedUrl;
  }
}
