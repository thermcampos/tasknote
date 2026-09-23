package br.com.tasknoteapp.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.tasknoteapp.server.exception.EmailAlreadyExistsException;
import br.com.tasknoteapp.server.exception.RequestValidationException;
import br.com.tasknoteapp.server.request.LoginRequest;
import br.com.tasknoteapp.server.response.UserResponseWithToken;
import br.com.tasknoteapp.server.service.AuthService;
import java.time.LocalDateTime;
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
class AuthenticationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @Test
  @DisplayName("Sign up happy path should succeed")
  void signup_happyPath_shouldSucceed() throws Exception {
    LoginRequest request =
        new LoginRequest("user@domain.com", "abcde123456", "abcde123456", "en");
    final String token = "xaxbxcxdx1x2x3A@";

    UserResponseWithToken response =
        new UserResponseWithToken(
            123L,
            null,
            request.email(),
            false,
            LocalDateTime.now(),
            null,
            null,
            null,
            token,
            "en");
    when(authService.signUpNewUser(request)).thenReturn(response);

    String jsonString =
        """
        {
          "email": "user@domain.com",
          "password": "abcde123456",
          "passwordAgain": "abcde123456",
          "timezone": "UTC"
        }
        """;

    mockMvc
        .perform(
            put("/auth/sign-up")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonString))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("Sign up bad email request should fail")
  void signup_badEmailRequest_shouldFail() throws Exception {
    LoginRequest request =
        new LoginRequest("user@domain..com", "abcde123456", "abcde123456", "en");

    when(authService.signUpNewUser(request))
        .thenThrow(new RequestValidationException("email", "Invalid email"));

    String jsonString =
        """
        {
          "email": "user@domain..com",
          "password": "abcde123456",
          "passwordAgain": "abcde123456",
          "lang": "en",
          "timezone": "UTC"
        }
        """;

    mockMvc
        .perform(
            put("/auth/sign-up")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonString))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @DisplayName("Sign up email already exists should fail")
  void signup_userAlreadyExists_shouldFail() throws Exception {
    LoginRequest request =
        new LoginRequest("user@domain.com", "abcde123456", "abcde123456", "en");

    when(authService.signUpNewUser(request)).thenThrow(new EmailAlreadyExistsException());

    String jsonString =
        """
        {
          "email": "user@domain.com",
          "password": "abcde123456",
          "passwordAgain": "abcde123456",
          "lang": "en",
          "timezone": "UTC"
        }
        """;

    mockMvc
        .perform(
            put("/auth/sign-up")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonString))
        .andExpect(status().isConflict())
        .andReturn();
  }

  @Test
  @DisplayName("Sign in happy path should succeed")
  void signin_happyPath_shouldSucceed() throws Exception {
    LoginRequest request =
        new LoginRequest("user@domain.com", "abcde123456", "abcde123456", "en");
    final String token = "xaxbxcxdx1x2x3A@";

    UserResponseWithToken response =
        new UserResponseWithToken(
            123L,
            null,
            request.email(),
            false,
            LocalDateTime.now(),
            null,
            null,
            null,
            token,
            "en");
    when(authService.signInUser(request)).thenReturn(response);

    String jsonString =
        """
        {
          "email": "user@domain.com",
          "password": "abcde123456",
          "passwordAgain": "abcde123456",
          "lang": "en",
          "timezone": "UTC"
        }
        """;

    mockMvc
        .perform(
            post("/auth/sign-in")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonString))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(response.userId()))
        .andExpect(jsonPath("$.email").value(response.email()))
        .andExpect(jsonPath("$.admin").value(response.admin()))
        .andExpect(jsonPath("$.token").value(token))
        .andReturn();
  }

  @Test
  @DisplayName("Sign in invalid credentials should fail")
  void signIn_invalidCredentials_shouldFail() throws Exception {
    LoginRequest request =
        new LoginRequest("user@domain.com", "abcde123456", "abcde123456", "en");

    when(authService.signInUser(request)).thenReturn(null);

    String jsonString =
        """
        {
          "email": "user@domain.com",
          "password": "abcde123456",
          "timezone": "UTC"
        }
        """;

    mockMvc
        .perform(
            post("/auth/sign-in")
                .with(csrf().asHeader())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonString))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }
}
