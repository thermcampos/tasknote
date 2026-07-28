package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.request.NotePatchRequest;
import br.com.tasknoteapp.server.request.NoteRequest;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.response.NoteUrlResponse;
import br.com.tasknoteapp.server.service.NoteService;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NoteService noteService;

  @Test
  @DisplayName("Get all notes with some notes found should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getAllNotes_notesFound_shouldSucceed() throws Exception {
    NoteUrlResponse noteUrl = new NoteUrlResponse(111L, "https://test.com");
    NoteResponse note =
        new NoteResponse(111L, "title", "description", "https://test.com", null, List.of("tag"), false, null, false);

    when(noteService.getAllNotes()).thenReturn(List.of(note));

    mockMvc
        .perform(
            get("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(note.id()))
        .andExpect(jsonPath("$[0].title").value(note.title()))
        .andExpect(jsonPath("$[0].description").value(note.description()))
        .andExpect(jsonPath("$[0].url").value(noteUrl.url()))
        .andReturn();
  }

  @Test
  @DisplayName("Get all notes without notes found should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getAllNotes_noNotesFound_shouldSucceed() throws Exception {
    when(noteService.getAllNotes()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", Matchers.empty()))
        .andReturn();
  }

  @Test
  @DisplayName("Get all notes with 401 unauthorized request should fail")
  void getAllNotes_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            get("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Patch a note via patch request happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void patchNote_happyPath_shouldSucceed() throws Exception {
    Long noteId = 123L;
    NotePatchRequest patchRequest =
        new NotePatchRequest("New title", "New description", null, List.of("tag"));

    NoteResponse response =
        new NoteResponse(
            noteId,
            patchRequest.title(),
            patchRequest.description(),
            null,
            null,
            List.of("tag"),
            false,
            null,
            false);

    when(noteService.patchNote(noteId, patchRequest)).thenReturn(response);

    final String payloadJson =
        """
        {
          "title": "New title",
          "description": "New description",
          "url": null,
          "tags": ["tag"]
        }
        """;

    mockMvc
        .perform(
            patch("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id()))
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.description").value(response.description()))
        .andExpect(jsonPath("$.url", Matchers.nullValue()))
        .andReturn();
  }

  @Test
  @DisplayName("Patch a note via patch request with not found id should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void patchNote_notFound_shouldFail() throws Exception {
    Long noteId = 123L;
    NotePatchRequest patchRequest =
        new NotePatchRequest("New title", "New description", null, List.of("tag"));

    when(noteService.patchNote(noteId, patchRequest)).thenThrow(new NoteNotFoundException());

    final String payloadJson =
        """
        {
          "title": "New title",
          "description": "New description",
          "url": null,
          "tags": ["tag"]
        }
        """;

    mockMvc
        .perform(
            patch("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isNotFound())
        .andReturn();
  }

  @Test
  @DisplayName("Patch a note via patch request with 401 unauthorized exception should fail")
  void patchNote_unauthorized_shouldFail() throws Exception {
    Long noteId = 123L;

    final String payloadJson =
        """
        {
          "title": "New title",
          "description": "New description",
          "urls": []
        }
        """;

    mockMvc
        .perform(
            patch("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Post create note happy path should succeed and return 201")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void postNotes_happyPath_shouldSucceed() throws Exception {
    NoteRequest request = new NoteRequest("Title", "Description", null, List.of("tag"));

    NoteResponse entity = new NoteResponse(1L, request.title(), request.description(),
        null, null, List.of("tag"), false, null, false);

    when(noteService.createNote(request)).thenReturn(entity);

    final String payloadJson =
        """
        {
          "title": "Title",
          "description": "Description",
          "url": null,
          "tags": ["tag"]
        }
        """;

    mockMvc
        .perform(
            post("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(entity.id()))
        .andExpect(jsonPath("$.title").value(entity.title()))
        .andExpect(jsonPath("$.description").value(entity.description()))
        .andExpect(jsonPath("$.url", Matchers.nullValue()))
        .andReturn();
  }

  @Test
  @DisplayName("Post create note with missing information should fail with 400 bad request")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void postNotes_missingInformation_shouldFail() throws Exception {
    final String payloadJson =
        """
        {
          "description": "Description"
        }
        """;

    mockMvc
        .perform(
            post("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @DisplayName("Post create note with 401 unauthorized request should fail")
  void postNotes_unauthorized_shouldFail() throws Exception {
    final String payloadJson =
        """
        {
          "description": "Any description here"
        }
        """;

    mockMvc
        .perform(
            post("/rest/notes")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(payloadJson))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Delete note request happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteNote_happyPath_shouldSucceed() throws Exception {
    final Long noteId = 453L;

    doNothing().when(noteService).deleteNote(noteId);

    mockMvc
        .perform(
            delete("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("Delete note with 401 unauthorized should fail")
  void deleteNote_unauthorized_shouldFail() throws Exception {
    final Long noteId = 453L;

    mockMvc
        .perform(
            delete("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Delete note with 404 request not found should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteNote_notFound_shouldFail() throws Exception {
    final Long noteId = 453L;

    doThrow(new NoteNotFoundException()).when(noteService).deleteNote(noteId);

    mockMvc
        .perform(
            delete("/rest/notes/{id}", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andReturn();
  }

  @Test
  @DisplayName("Share note happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void shareNote_happyPath_shouldSucceed() throws Exception {
    final Long noteId = 1L;
    final String token = "test-token-uuid";
    NoteResponse response =
        new NoteResponse(
            noteId, "title", "description", null, null, List.of("tag"), true, token, false);

    when(noteService.shareNote(noteId)).thenReturn(response);

    mockMvc
        .perform(
            put("/rest/notes/{id}/share", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shared").value(true))
        .andExpect(jsonPath("$.shareToken").value(token))
        .andReturn();
  }

  @Test
  @DisplayName("Share note with 401 unauthorized should fail")
  void shareNote_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            put("/rest/notes/{id}/share", 1L)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Unshare note happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void unshareNote_happyPath_shouldSucceed() throws Exception {
    final Long noteId = 1L;
    NoteResponse response =
        new NoteResponse(
            noteId, "title", "description", null, null, List.of("tag"), false, null, false);

    when(noteService.unshareNote(noteId)).thenReturn(response);

    mockMvc
        .perform(
            put("/rest/notes/{id}/unshare", noteId)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shared").value(false))
        .andExpect(jsonPath("$.shareToken", Matchers.nullValue()))
        .andReturn();
  }

  @Test
  @DisplayName("Unshare note with 401 unauthorized should fail")
  void unshareNote_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            put("/rest/notes/{id}/unshare", 1L)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }
}
