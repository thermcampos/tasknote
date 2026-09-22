package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.NoteUrl;
import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.entity.UserPwdLimit;
import br.com.tasknoteapp.server.exception.InvalidCredentialsException;
import br.com.tasknoteapp.server.exception.MaxLoginLimitAttemptException;
import br.com.tasknoteapp.server.exception.NoteNotArchivedException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.repository.TaskRepository;
import br.com.tasknoteapp.server.repository.UserPwdLimitRepository;
import br.com.tasknoteapp.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountDeletionIntTest {

  private static final String RAW_PASSWORD = "a1b2c3d4f5g6";

  @Autowired private UserSessionService userSessionService;

  @Autowired private NoteService noteService;

  @Autowired private UserRepository userRepository;

  @Autowired private NoteRepository noteRepository;

  @Autowired private NoteUrlRepository noteUrlRepository;

  @Autowired private TaskRepository taskRepository;

  @Autowired private TagRepository tagRepository;

  @Autowired private UserPwdLimitRepository userPwdLimitRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private AuthService authService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setEmail("account-deletion@domain.com");
    user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
    user.setAdmin(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setLastPasswordChange(LocalDateTime.now());
    user = userRepository.save(user);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Delete account with mixed archived and non-archived notes should remove all data")
  void deleteAccount_mixedArchivedNotes_shouldRemoveAllUserData() {
    Note activeNote = new Note(
        null,
        user.getId(),
        "Not archived",
        "Active note",
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.FALSE
    );
    activeNote = noteRepository.save(activeNote);

    NoteUrl noteUrl = new NoteUrl(
        null,
        activeNote.id(),
        "http://example.com"
    );
    noteUrlRepository.save(noteUrl);

    Note archivedNote = new Note(
        null,
        user.getId(),
        "Archived",
        "Archived note",
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.TRUE
    );
    archivedNote = noteRepository.save(archivedNote);

    Task task = new Task(
        null,
        user.getId(),
        "A task",
        Boolean.FALSE,
        LocalDateTime.now(),
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE
    );
    taskRepository.save(task);

    Long userId = user.getId();
    final Long activeNoteId = activeNote.id();

    userSessionService.deleteCurrentUserAccount();

    assertTrue(userRepository.findById(userId).isEmpty());
    assertTrue(noteRepository.findAllByUserId(userId).isEmpty());
    assertTrue(noteUrlRepository.findByNoteId(activeNoteId).isEmpty());
    assertTrue(taskRepository.findAllByUserId(userId).isEmpty());
    assertFalse(tagRepository.userHasAnyTags(userId));
    assertTrue(
        userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(userId).isEmpty());
  }

  @Test
  @DisplayName("Delete account with wrong password should fail, keep data and record the attempt")
  void deleteAccount_wrongPassword_shouldFailAndRecordAttempt() {
    Long userId = user.getId();

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.verifyCurrentPassword(user, "wrong-password");
          userSessionService.deleteCurrentUserAccount();
        });

    assertTrue(userRepository.findById(userId).isPresent());

    List<UserPwdLimit> attempts =
        userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(userId);
    assertEquals(1, attempts.size());
  }

  @Test
  @DisplayName("Delete account with three recent failed attempts should be rejected by rate limit")
  void deleteAccount_maxAttempts_shouldFail() {
    Long userId = user.getId();

    for (int i = 0; i < 3; i++) {
      UserPwdLimit attempt = new UserPwdLimit(
          null,
          LocalDateTime.now().minusSeconds(30),
          user.getId()
      );
      userPwdLimitRepository.save(attempt);
    }

    assertThrows(
        MaxLoginLimitAttemptException.class,
        () -> {
          authService.verifyCurrentPassword(user, RAW_PASSWORD);
          userSessionService.deleteCurrentUserAccount();
        });

    assertTrue(userRepository.findById(userId).isPresent());
  }

  @Test
  @DisplayName("Single-note delete should still reject non-archived notes")
  void deleteNote_nonArchived_shouldStillThrow() {
    Note activeNote = new Note(
        null,
        user.getId(),
        "Not archived",
        "Active note",
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.FALSE
    );
    activeNote = noteRepository.save(activeNote);

    Long noteId = activeNote.id();

    assertThrows(NoteNotArchivedException.class, () -> noteService.deleteNote(noteId));

    assertTrue(noteRepository.findById(noteId).isPresent());
  }

  @Test
  @DisplayName("Single-note delete of archived note should still work")
  void deleteNote_archived_shouldSucceed() {
    Note archivedNote = new Note(
        null,
        user.getId(),
        "Archived",
        "Archived note",
        LocalDateTime.now(),
        Boolean.FALSE,
        null,
        Boolean.TRUE
    );
    archivedNote = noteRepository.save(archivedNote);

    Long noteId = archivedNote.id();

    noteService.deleteNote(noteId);

    assertTrue(noteRepository.findById(noteId).isEmpty());
    assertEquals(0, noteRepository.findAllByUserId(user.getId()).size());
  }
}
