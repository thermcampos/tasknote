package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.NoteUrl;
import br.com.tasknoteapp.server.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@RepositoryIntTest
@Sql(scripts = {"classpath:sql/NoteUrlRepositoryTest.sql"})
class NoteUrlRepositoryIntTest {

  @Autowired NoteUrlRepository noteUrlRepository;

  @Autowired NoteRepository noteRepository;

  @Autowired UserRepository userRepository;

  private static final String USER_EMAIL = "test@domain.com";

  @Test
  @DisplayName("Find all note url by note id")
  void findAllByNoteIdIntTest() {
    Long noteId = findNoteId("Note with urls");

    List<NoteUrl> noteUrls = noteUrlRepository.findAllByNoteId(noteId);

    Assertions.assertEquals(2, noteUrls.size());
    Assertions.assertTrue(noteUrls.stream().allMatch(nu -> nu.noteId().equals(noteId)));
  }

  @Test
  @DisplayName("Find all note url by note id without urls returns empty")
  void findAllByNoteIdWithoutUrlsIntTest() {
    Long noteId = findNoteId("Note without urls");

    List<NoteUrl> noteUrls = noteUrlRepository.findAllByNoteId(noteId);

    Assertions.assertTrue(noteUrls.isEmpty());
  }

  @Test
  @DisplayName("Save all note urls")
  void saveAllIntTest() {
    Long noteId = findNoteId("Note without urls");

    int created = noteUrlRepository.saveAll(
        List.of(new NoteUrl(noteId, "openjdk.org"), new NoteUrl(noteId, "graalvm.org")));

    Assertions.assertEquals(2, created);
    Assertions.assertEquals(2, noteUrlRepository.findAllByNoteId(noteId).size());
  }

  @Test
  @DisplayName("Delete one note url by note id and url")
  void deleteByNoteIdAndUrlIntTest() {
    Long noteId = findNoteId("Note with urls");

    int deleted = noteUrlRepository.deleteByNoteIdAndUrl(noteId, "debian.org");

    Assertions.assertEquals(1, deleted);
    List<NoteUrl> noteUrls = noteUrlRepository.findAllByNoteId(noteId);
    Assertions.assertEquals(1, noteUrls.size());
    Assertions.assertEquals("postgresql.org", noteUrls.getFirst().url());
  }

  @Test
  @DisplayName("Delete all note url by note id")
  void deleteAllByNoteIdIntTest() {
    Long noteId = findNoteId("Note with urls");

    noteUrlRepository.deleteAllByNoteId(noteId);

    Assertions.assertTrue(noteUrlRepository.findAllByNoteId(noteId).isEmpty());
  }

  private Long findNoteId(String description) {
    Optional<User> user = userRepository.findByEmail(USER_EMAIL);
    Assertions.assertFalse(user.isEmpty());

    List<Note> notes = noteRepository.findAllByUserId(user.get().getId());
    Assertions.assertFalse(notes.isEmpty());

    Optional<Note> note =
        notes.stream().filter(n -> n.description().equals(description)).findFirst();
    Assertions.assertFalse(note.isEmpty());

    return note.get().id();
  }
}
