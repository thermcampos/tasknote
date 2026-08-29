package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.tasknoteapp.server.entity.NoteEntity;
import br.com.tasknoteapp.server.entity.NoteUrlEntity;
import br.com.tasknoteapp.server.entity.TagEntity;
import br.com.tasknoteapp.server.entity.TaskEntity;
import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.entity.UserPwdLimitEntity;
import br.com.tasknoteapp.server.exception.InvalidCredentialsException;
import br.com.tasknoteapp.server.exception.MaxLoginLimitAttemptException;
import br.com.tasknoteapp.server.exception.NoteArchivedException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.repository.TaskRepository;
import br.com.tasknoteapp.server.repository.UserPwdLimitRepository;
import br.com.tasknoteapp.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

  private UserEntity user;

  @BeforeEach
  void setUp() {
    user = new UserEntity();
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
    TagEntity tag = tagRepository.save(new TagEntity("work", user));

    NoteEntity activeNote = new NoteEntity();
    activeNote.setTitle("Active note");
    activeNote.setDescription("Not archived");
    activeNote.setUser(user);
    activeNote.setLastUpdate(LocalDateTime.now());
    activeNote.setTags(Set.of(tag));
    activeNote = noteRepository.save(activeNote);

    NoteUrlEntity noteUrl = new NoteUrlEntity();
    noteUrl.setUrl("http://example.com");
    noteUrl.setNote(activeNote);
    noteUrlRepository.save(noteUrl);

    NoteEntity archivedNote = new NoteEntity();
    archivedNote.setTitle("Archived note");
    archivedNote.setDescription("Archived");
    archivedNote.setUser(user);
    archivedNote.setLastUpdate(LocalDateTime.now());
    archivedNote.setArchived(true);
    archivedNote.setTags(Set.of(tag));
    archivedNote = noteRepository.save(archivedNote);

    TaskEntity task = new TaskEntity();
    task.setDescription("A task");
    task.setCompleted(false);
    task.setUser(user);
    task.setLastUpdate(LocalDateTime.now());
    task.setTags(Set.of(tag));
    taskRepository.save(task);

    Long userId = user.getId();
    final Long activeNoteId = activeNote.getId();

    userSessionService.deleteCurrentUserAccount();

    assertTrue(userRepository.findById(userId).isEmpty());
    assertTrue(noteRepository.findAllByUser_id(userId).isEmpty());
    assertTrue(noteUrlRepository.findByNote_id(activeNoteId).isEmpty());
    assertTrue(taskRepository.findAllByUser_id(userId).isEmpty());
    assertTrue(tagRepository.findAllByUser_idOrderByNameAsc(userId).isEmpty());
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

    List<UserPwdLimitEntity> attempts =
        userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(userId);
    assertEquals(1, attempts.size());
  }

  @Test
  @DisplayName("Delete account with three recent failed attempts should be rejected by rate limit")
  void deleteAccount_maxAttempts_shouldFail() {
    Long userId = user.getId();

    for (int i = 0; i < 3; i++) {
      UserPwdLimitEntity attempt = new UserPwdLimitEntity();
      attempt.setWhenHappened(LocalDateTime.now().minusSeconds(30));
      attempt.setUser(user);
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
    NoteEntity activeNote = new NoteEntity();
    activeNote.setTitle("Active note");
    activeNote.setDescription("Not archived");
    activeNote.setUser(user);
    activeNote.setLastUpdate(LocalDateTime.now());
    activeNote = noteRepository.save(activeNote);

    Long noteId = activeNote.getId();

    assertThrows(NoteArchivedException.class, () -> noteService.deleteNote(noteId));

    assertTrue(noteRepository.findById(noteId).isPresent());
  }

  @Test
  @DisplayName("Single-note delete of archived note should still work")
  void deleteNote_archived_shouldSucceed() {
    NoteEntity archivedNote = new NoteEntity();
    archivedNote.setTitle("Archived note");
    archivedNote.setDescription("Archived");
    archivedNote.setUser(user);
    archivedNote.setLastUpdate(LocalDateTime.now());
    archivedNote.setArchived(true);
    archivedNote = noteRepository.save(archivedNote);

    Long noteId = archivedNote.getId();

    noteService.deleteNote(noteId);

    assertTrue(noteRepository.findById(noteId).isEmpty());
    assertEquals(0, noteRepository.findAllByUser_id(user.getId()).size());
  }
}
