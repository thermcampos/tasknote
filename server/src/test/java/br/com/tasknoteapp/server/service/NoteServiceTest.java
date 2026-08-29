package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.NoteEntity;
import br.com.tasknoteapp.server.entity.NoteUrlEntity;
import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.repository.NoteRepository;
import br.com.tasknoteapp.server.repository.NoteUrlRepository;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.request.NotePatchRequest;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

  @Mock private NoteRepository noteRepository;

  @Mock private AuthService authService;

  @Mock private AuthUtil authUtil;

  @Mock private NoteUrlRepository noteUrlRepository;

  @Mock private TagRepository tagRepository;

  @InjectMocks private NoteService noteService;

  private UserEntity user;
  private NoteEntity note;
  private NoteRequest noteRequest;
  private NotePatchRequest notePatchRequest;

  @BeforeEach
  void setUp() {
    user = new UserEntity();
    user.setId(1L);
    user.setEmail("test@example.com");

    note = new NoteEntity();
    note.setId(1L);
    note.setTitle("Test Note");
    note.setDescription("Test Description");
    note.setUser(user);

    noteRequest =
        new NoteRequest("Test Note", "Test Description", "http://example.com", List.of("tag"));
    notePatchRequest =
        new NotePatchRequest(
            "Updated Note", "Updated Description", "http://example.com", List.of("tag"));
  }

  @Test
  void getAllNotes() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findAllByUser_id(user.getId())).thenReturn(List.of(note));

    List<NoteResponse> notes = noteService.getAllNotes();

    assertEquals(1, notes.size());
    assertEquals("Test Note", notes.get(0).title());
    verify(noteRepository, times(1)).findAllByUser_id(user.getId());
  }

  @Test
  void getNoteById() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findById(note.getId())).thenReturn(Optional.of(note));

    NoteResponse noteResponse = noteService.getNoteById(note.getId());

    assertEquals("Test Note", noteResponse.title());
    verify(noteRepository, times(1)).findById(note.getId());
  }

  @Test
  void getNoteById_NotFound() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findById(note.getId())).thenReturn(Optional.empty());
    Long noteId = note.getId();

    assertThrows(NoteNotFoundException.class, () -> noteService.getNoteById(noteId));
  }

  @Test
  void createNote_withExistingCount() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.save(any(NoteEntity.class))).thenReturn(note);
    when(noteUrlRepository.save(any(NoteUrlEntity.class))).thenReturn(new NoteUrlEntity());
    when(tagRepository.findByNameAndUser_id(anyString(), eq(user.getId())))
        .thenReturn(Optional.of(new br.com.tasknoteapp.server.entity.TagEntity("tag", user)));

    NoteResponse createdNote = noteService.createNote(noteRequest);

    assertEquals("Test Note", createdNote.title());
    verify(noteRepository, times(1)).save(any(NoteEntity.class));
    verify(noteUrlRepository, times(1)).save(any(NoteUrlEntity.class));
  }

  @Test
  void patchNote() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.of(note));
    when(noteRepository.save(any(NoteEntity.class))).thenReturn(note);
    when(tagRepository.findByNameAndUser_id(anyString(), eq(user.getId())))
        .thenReturn(Optional.of(new br.com.tasknoteapp.server.entity.TagEntity("tag", user)));

    NoteResponse patchedNote = noteService.patchNote(note.getId(), notePatchRequest);

    assertEquals("Updated Note", patchedNote.title());
    verify(noteRepository, times(1)).findByIdAndUser_id(note.getId(), user.getId());
    verify(noteRepository, times(1)).save(any(NoteEntity.class));
  }

  @Test
  void patchNote_notFound() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.empty());
    Long noteId = note.getId();

    assertThrows(
        NoteNotFoundException.class, () -> noteService.patchNote(noteId, notePatchRequest));
  }

  @Test
  void deleteNote() {
    note.setArchived(true);
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.of(note));

    noteService.deleteNote(note.getId());

    verify(noteRepository, times(1)).findByIdAndUser_id(note.getId(), user.getId());
    verify(noteRepository, times(1)).delete(note);
  }

  @Test
  void deleteAllNotesForCurrentUser() {
    NoteEntity archivedNote = new NoteEntity();
    archivedNote.setId(2L);
    archivedNote.setTitle("Archived Note");
    archivedNote.setDescription("Archived Description");
    archivedNote.setUser(user);
    archivedNote.setArchived(true);

    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findAllByUser_id(user.getId())).thenReturn(List.of(note, archivedNote));

    noteService.deleteAllNotesForCurrentUser();

    verify(noteUrlRepository, times(1)).deleteByNote_id(note.getId());
    verify(noteUrlRepository, times(1)).deleteByNote_id(archivedNote.getId());
    verify(noteRepository, times(1)).delete(note);
    verify(noteRepository, times(1)).delete(archivedNote);
    verify(tagRepository, times(1)).deleteOrphanedTags(user.getId());
  }

  @Test
  void searchNotes() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findAllBySearchTerm(anyString(), eq(user.getId())))
        .thenReturn(List.of(note));

    List<NoteResponse> notes = noteService.searchNotes("Test");

    assertEquals(1, notes.size());
    assertEquals("Test Note", notes.get(0).title());
    verify(noteRepository, times(1)).findAllBySearchTerm(anyString(), eq(user.getId()));
  }

  @Test
  void shareNote() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.of(note));
    when(noteRepository.save(any(NoteEntity.class))).thenReturn(note);

    NoteResponse response = noteService.shareNote(note.getId());

    assertEquals("Test Note", response.title());
    verify(noteRepository, times(1)).save(any(NoteEntity.class));
  }

  @Test
  void shareNote_notFound() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.empty());
    Long noteId = note.getId();

    assertThrows(NoteNotFoundException.class, () -> noteService.shareNote(noteId));
  }

  @Test
  void unshareNote() {
    note.setShared(true);
    note.setShareToken("some-token");
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.of(note));
    when(noteRepository.save(any(NoteEntity.class))).thenReturn(note);

    NoteResponse response = noteService.unshareNote(note.getId());

    assertEquals("Test Note", response.title());
    verify(noteRepository, times(1)).save(any(NoteEntity.class));
  }

  @Test
  void unshareNote_notFound() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(user.getEmail()));
    when(authService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(noteRepository.findByIdAndUser_id(note.getId(), user.getId()))
        .thenReturn(Optional.empty());
    Long noteId = note.getId();

    assertThrows(NoteNotFoundException.class, () -> noteService.unshareNote(noteId));
  }

  @Test
  void getSharedNote() {
    final String token = "share-token-123";
    note.setShared(true);
    note.setShareToken(token);
    when(noteRepository.findByShareToken(token)).thenReturn(Optional.of(note));

    NoteResponse response = noteService.getSharedNote(token);

    assertEquals("Test Note", response.title());
    verify(noteRepository, times(1)).findByShareToken(token);
  }

  @Test
  void getSharedNote_notFound() {
    when(noteRepository.findByShareToken("bad-token")).thenReturn(Optional.empty());

    assertThrows(NoteNotFoundException.class, () -> noteService.getSharedNote("bad-token"));
  }

  @Test
  void getSharedNote_notShared() {
    final String token = "share-token-456";
    note.setShared(false);
    note.setShareToken(token);
    when(noteRepository.findByShareToken(token)).thenReturn(Optional.of(note));

    assertThrows(NoteNotFoundException.class, () -> noteService.getSharedNote(token));
  }
}
