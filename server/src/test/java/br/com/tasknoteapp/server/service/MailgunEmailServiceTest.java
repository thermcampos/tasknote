package br.com.tasknoteapp.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Test class for MailgunEmailService using RestClient. */
@ExtendWith(MockitoExtension.class)
class MailgunEmailServiceTest {

  @Mock private RestClient restClient;
  @Mock private RestClient.Builder restClientBuilder;
  @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RestClient.RequestBodySpec requestBodySpec;
  @Mock private RestClient.ResponseSpec responseSpec;

  private MailgunEmailService mailgunEmailService;

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
    mailgunEmailService =
        new MailgunEmailService(apiKey, domain, sender, target, restClientBuilder);
  }

  private void setupMockChain() {
    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
  }

  @Test
  void testSendResetPassword() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setResetToken("reset-token");

    setupMockChain();

    mailgunEmailService.sendResetPassword(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }

  @Test
  void testSendPasswordResetConfirmation() {
    User user = new User();
    user.setEmail("test@example.com");

    setupMockChain();

    mailgunEmailService.sendPasswordResetConfirmation(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }

  @Test
  void testSendNewUser() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setEmailUuid(java.util.UUID.randomUUID());

    setupMockChain();

    mailgunEmailService.sendNewUser(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }

  @Test
  void testSendEmailHandlesHttpClientErrorException() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setResetToken("reset-token");

    setupMockChain();
    when(responseSpec.toBodilessEntity())
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

    mailgunEmailService.sendResetPassword(user);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }

  @Test
  void testSendEmailChanged() {
    User user = new User();
    user.setEmail("test@example.com");

    setupMockChain();

    String oldEmail = "old@example.com";

    mailgunEmailService.sendEmailChangedNotification(user, oldEmail);

    verify(restClient, times(1)).post();
    verify(responseSpec, times(1)).toBodilessEntity();
  }
}
