package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Test class for ResendEmailService using RestClient. */
@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

  @Mock private RestClient restClient;
  @Mock private RestClient.Builder restClientBuilder;
  @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RestClient.RequestBodySpec requestBodySpec;
  @Mock private RestClient.ResponseSpec responseSpec;

  private ResendEmailService resendEmailService;

  @BeforeEach
  void setUp() {
    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultStatusHandler(any(), any())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultHeaders(any())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    String apiKey = "abx123";
    String domain = "domain.com";
    String sender = "no-reply@domain.com";
    String target = "development";
    resendEmailService = new ResendEmailService(apiKey, domain, sender, target, restClientBuilder);
  }

  private void setupMockChain() {
    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
  }

  private ResendEmailRequest captureEmailRequest() {
    ArgumentCaptor<ResendEmailRequest> captor = ArgumentCaptor.forClass(ResendEmailRequest.class);
    verify(requestBodySpec, times(1)).body(captor.capture());
    return captor.getValue();
  }

  @Test
  void testSendResetPassword() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setResetToken("reset-token");

    setupMockChain();

    resendEmailService.sendResetPassword(user);

    verify(restClient, times(1)).post();
    verify(requestBodyUriSpec, times(1)).uri("/emails");
    verify(responseSpec, times(1)).toBodilessEntity();

    ResendEmailRequest request = captureEmailRequest();
    assertEquals("TaskNote App <no-reply@domain.com>", request.from());
    assertEquals("test@example.com", request.to().getFirst());
    assertEquals("TaskNote App password reset", request.subject());
    assertNull(request.cc());
    assertTrue(
        request.html().contains("http://localhost:5000/finish-reset-password?token=reset-token"));
  }

  @Test
  void testSendPasswordResetConfirmation() {
    User user = new User();
    user.setEmail("test@example.com");

    setupMockChain();

    resendEmailService.sendPasswordResetConfirmation(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();

    ResendEmailRequest request = captureEmailRequest();
    assertEquals("test@example.com", request.to().getFirst());
    assertEquals("TaskNote App password confirmation", request.subject());
    assertNull(request.cc());
    assertTrue(request.html().contains("<html"));
  }

  @Test
  void testSendNewUser() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setEmailUuid(UUID.randomUUID());

    setupMockChain();

    resendEmailService.sendNewUser(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();

    ResendEmailRequest request = captureEmailRequest();
    assertEquals("TaskNote App confirmation email", request.subject());
    assertTrue(
        request
            .html()
            .contains(
                "http://localhost:5000/email-confirmation?identification="
                    + user.getEmailUuid()));
  }

  @Test
  void testSendEmailHandlesHttpClientErrorException() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setResetToken("reset-token");

    setupMockChain();
    when(responseSpec.toBodilessEntity())
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

    resendEmailService.sendResetPassword(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }

  @Test
  void testSendEmailChanged() {
    User user = new User();
    user.setEmail("test@example.com");

    setupMockChain();

    String oldEmail = "old@example.com";

    resendEmailService.sendEmailChangedNotification(user, oldEmail);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();

    ResendEmailRequest request = captureEmailRequest();
    assertEquals("TaskNote App email changed notification", request.subject());
    assertEquals(oldEmail, request.cc().getFirst());
    assertTrue(request.html().contains(oldEmail));
    assertTrue(request.html().contains("test@example.com"));
  }
}
