package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.service.AppVersionService;
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
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AppVersionService appVersionService;

  @Test
  @DisplayName("Health check without authentication should succeed")
  void health_withoutAuth_shouldSucceed() throws Exception {
    when(appVersionService.getVersion()).thenReturn("test-version");

    mockMvc
        .perform(get("/health").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application").value("UP"))
        .andExpect(jsonPath("$.version").value("test-version"))
        .andExpect(jsonPath("$.database.status").value("UP"))
        .andReturn();
  }
}
