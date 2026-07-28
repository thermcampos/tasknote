package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.exception.NoteNotFoundException;
import br.com.tasknoteapp.server.response.NoteResponse;
import br.com.tasknoteapp.server.service.NoteService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicNoteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NoteService noteService;

  @Test
  @DisplayName("Get shared note by token happy path should succeed")
  void getSharedNote_happyPath_shouldSucceed() throws Exception {
    final String token = "test-share-token";
    NoteResponse response =
        new NoteResponse(
            1L, "title", "description", null, null, List.of("tag"), true, token, false);

    when(noteService.getSharedNote(token)).thenReturn(response);

    mockMvc
        .perform(
            get("/public/notes/{token}", token)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("title"))
        .andExpect(jsonPath("$.shared").value(true))
        .andExpect(jsonPath("$.shareToken").value(token))
        .andReturn();
  }

  @Test
  @DisplayName("Get shared note by token not found should fail with 404")
  void getSharedNote_notFound_shouldFail() throws Exception {
    final String token = "invalid-token";

    when(noteService.getSharedNote(token)).thenThrow(new NoteNotFoundException());

    mockMvc
        .perform(
            get("/public/notes/{token}", token)
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andReturn();
  }
}
