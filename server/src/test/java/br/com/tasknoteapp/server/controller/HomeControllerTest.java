package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.service.HomeService;
import java.util.List;
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
class HomeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HomeService homeService;

  @Test
  @DisplayName("Get task tags following the happy path it should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getTasksTags_happyPath_shouldSucceed() throws Exception {
    when(homeService.getTopTasksTag()).thenReturn(List.of("tag1", "tag2"));

    mockMvc
        .perform(
            get("/rest/home/tasks/tags")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("tag1"))
        .andExpect(jsonPath("$[1]").value("tag2"))
        .andReturn();
  }

  @Test
  @DisplayName("Get task tags with no tags found should succeed with empty list")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void getTasksTags_noTagsFound_shouldSucceed() throws Exception {
    when(homeService.getTopTasksTag()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/rest/home/tasks/tags")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()))
        .andReturn();
  }

  @Test
  @DisplayName("Get task tags not authorized it should fail")
  void getTasksTags_notAuthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            get("/rest/home/tasks/tags")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }
}
