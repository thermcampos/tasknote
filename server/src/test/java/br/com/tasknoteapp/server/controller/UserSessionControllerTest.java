package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.response.JwtAuthenticationResponse;
import br.com.tasknoteapp.server.response.UserResponse;
import br.com.tasknoteapp.server.service.AuthService;
import br.com.tasknoteapp.server.service.UserSessionService;
import java.util.Optional;
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
class UserSessionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserSessionService userSessionService;

  @MockitoBean private AuthService authService;

  @Test
  @DisplayName("Refresh happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void refresh_happyPath_shouldSucceed() throws Exception {
    JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse("axbxdxdc123456");
    when(userSessionService.refreshUserSession()).thenReturn(authResponse);

    mockMvc
        .perform(
            get("/rest/user-sessions/refresh")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(authResponse.token()))
        .andReturn();
  }

  @Test
  @DisplayName("Refresh with 401 unauthorized request should fail")
  void refresh_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            get("/rest/user-sessions/refresh")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("Delete account happy path should succeed")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteAccount_happyPath_shouldSucceed() throws Exception {
    UserEntity user = new UserEntity();
    user.setId(1L);
    UserResponse response =
        new UserResponse(1L, "John", "email@test.com", false, null, null, null, null, "light");
    when(authService.getCurrentUser()).thenReturn(Optional.of(user));
    when(userSessionService.deleteCurrentUserAccount()).thenReturn(response);

    mockMvc
        .perform(
            post("/rest/user-sessions/delete-account")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"abcde123456A@\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  @DisplayName("Delete account without password should fail")
  @WithMockUser(username = "user@domain.com", password = "abcde123456A@")
  void deleteAccount_missingPassword_shouldFail() throws Exception {
    mockMvc
        .perform(
            post("/rest/user-sessions/delete-account")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @DisplayName("Delete account with 401 unauthorized request should fail")
  void deleteAccount_unauthorized_shouldFail() throws Exception {
    mockMvc
        .perform(
            post("/rest/user-sessions/delete-account")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"abcde123456A@\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }
}
