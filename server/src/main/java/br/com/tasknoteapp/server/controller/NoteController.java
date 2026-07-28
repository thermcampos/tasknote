package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.request.NotePatchRequest;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.service.NoteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This class provides resources to handle notes requests by the client. */
@RestController
@RequestMapping("/rest/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  /**
   * Get all notes.
   *
   * @return List of NoteResponse with all found notes and its URLs, if any.
   */
  @GetMapping
  public List<NoteResponse> getAllNotes() {
    return noteService.getAllNotes();
  }

  /**
   * Get a note by its ID.
   *
   * @param id Note identification.
   * @return NoteResponse with note data and its URLs, if any.
   * @throws NoteNotFoundException when note not found.
   */
  @GetMapping("/{id}")
  public NoteResponse getTaskById(@PathVariable Long id) {
    return noteService.getNoteById(id);
  }

  /**
   * Patch a note.
   *
   * @param id The note id to be patched.
   * @param noteRequest Note data to be patched, including optionally its URLs.
   * @return NoteResponse containing data that was updated.
   * @throws NoteNotFoundException when note not found.
   */
  @PatchMapping("/{id}")
  public ResponseEntity<NoteResponse> patchNote(
      @PathVariable Long id, @RequestBody @Valid NotePatchRequest noteRequest) {

    return ResponseEntity.ok(noteService.patchNote(id, noteRequest));
  }

  /**
   * Create a note.
   *
   * @param noteRequest Note data to be created, including optionally its URLs. Following RESTful
   *     API pattern from <a href="https://restfulapi.net/rest-put-vs-post/">docs</a>.
   * @return NoteResponse containing data that was created.
   */
  @PostMapping
  public ResponseEntity<NoteResponse> postNotes(@RequestBody @Valid NoteRequest noteRequest) {
    NoteResponse createdNote = noteService.createNote(noteRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);
  }

  /**
   * Delete a note given its ID.
   *
   * @param id Note identification.
   * @throws NoteNotFoundException when note not found.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
    noteService.deleteNote(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Share a note publicly.
   *
   * @param id Note identification.
   * @return NoteResponse containing the share token.
   * @throws NoteNotFoundException when note not found.
   */
  @PutMapping("/{id}/share")
  public ResponseEntity<NoteResponse> shareNote(@PathVariable Long id) {
    return ResponseEntity.ok(noteService.shareNote(id));
  }

  /**
   * Unshare a note, revoking public access.
   *
   * @param id Note identification.
   * @return NoteResponse with the updated note.
   * @throws NoteNotFoundException when note not found.
   */
  @PutMapping("/{id}/unshare")
  public ResponseEntity<NoteResponse> unshareNote(@PathVariable Long id) {
    return ResponseEntity.ok(noteService.unshareNote(id));
  }

  /**
   * Archive a note, disabling edits and revoking public sharing.
   *
   * @param id Note identification.
   * @return NoteResponse with the archived note.
   * @throws NoteNotFoundException when note not found.
   */
  @PutMapping("/{id}/archive")
  public ResponseEntity<NoteResponse> archiveNote(@PathVariable Long id) {
    return ResponseEntity.ok(noteService.archiveNote(id));
  }

  /**
   * Restore an archived note back to active state.
   *
   * @param id Note identification.
   * @return NoteResponse with the restored note.
   * @throws NoteNotFoundException when note not found.
   */
  @PutMapping("/{id}/restore")
  public ResponseEntity<NoteResponse> restoreNote(@PathVariable Long id) {
    return ResponseEntity.ok(noteService.restoreNote(id));
  }
}
