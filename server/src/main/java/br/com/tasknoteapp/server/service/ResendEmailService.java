package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.templates.EmailTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** This service handles email messages for Resend. */
@Service
public class ResendEmailService {

  private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class.getName());
  private final RestClient restClient;
  private final String targetEnv;
  private final String domain;
  private final String senderEmail;

  /**
   * Creates an instance of the Mail service class.
   *
   * @param apiKey The api key to send emails with.
   * @param domain The domain to send email from.
   * @param sender The from option.
   * @param targetEnv The environment.
   * @param restClientBuilder The rest client builder.
   */
  public ResendEmailService(
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.domain}") String domain,
      @Value("${resend.sender-email}") String sender,
      @Value("${br.com.tasknote.server.target-env}") String targetEnv,
      RestClient.Builder restClientBuilder) {
    this.domain = domain;
    this.senderEmail = sender;
    this.targetEnv = targetEnv;

    if (apiKey != null && apiKey.length() > 6) {
      logger.info(
          "Resend API Key loaded: {}...{}",
          apiKey.substring(0, 3),
          apiKey.substring(apiKey.length() - 3));
    } else {
      logger.warn("Resend API Key is missing or too short!");
    }

    this.restClient =
        restClientBuilder
            .baseUrl("https://api.resend.com")
            .defaultStatusHandler(
                HttpStatusCode::isError,
                (request, response) ->
                    logger.error(
                        "Resend API Error: {} {}",
                        response.getStatusCode(),
                        response.getStatusText()))
            .defaultHeaders(headers -> headers.setBearerAuth(apiKey))
            .build();
  }

  /**
   * Send new users a message confirming their account.
   *
   * @param user The user that should be addressed the message.
   */
  public void sendNewUser(User user) {
    logger.info("Sending message confirming user email address.");

    String to = user.getEmail();
    String subject = "TaskNote App confirmation email";
    String link = getBaseUrl() + "/email-confirmation?identification=%s";

    logger.info("New user link: {}", link);

    Map<String, String> variables = new HashMap<>();
    variables.put("CONFIRMATION_LINK", String.format(link, user.getEmailUuid().toString()));

    sendEmail(to, subject, EmailTemplate.SIGN_UP, variables);
  }

  /**
   * Send a password reset link.
   *
   * @param user The user that should be addressed the message.
   */
  public void sendResetPassword(User user) {
    logger.info("Sending message with password reset link");

    String to = user.getEmail();
    String subject = "TaskNote App password reset";
    String link = getBaseUrl() + "/finish-reset-password?token=%s";

    logger.info("Password reset link: {}", link);

    Map<String, String> variables = new HashMap<>();
    variables.put("RESET_LINK", String.format(link, user.getResetToken()));

    sendEmail(to, subject, EmailTemplate.PASSWORD_RESET, variables);
  }

  /**
   * Send a confirmation for the password change.
   *
   * @param user The user that should be addressed the message.
   */
  public void sendPasswordResetConfirmation(User user) {
    logger.info("Sending message with password reset confirmation");

    String to = user.getEmail();
    String subject = "TaskNote App password confirmation";

    sendEmail(to, subject, EmailTemplate.PASSWORD_RESET_CONFIRM, Map.of());
  }

  /**
   * Send a notification about the changed email for both previous and new email.
   *
   * @param user The user that should be addressed the message.
   * @param oldEmail The user previous email
   */
  public void sendEmailChangedNotification(User user, String oldEmail) {
    logger.info("Sending message with changed email notification");

    Map<String, String> variables = new HashMap<>();
    variables.put("EMAIL_FROM", oldEmail);
    variables.put("EMAIL_TO", user.getEmail());

    String subject = "TaskNote App email changed notification";

    sendEmail(user.getEmail(), subject, EmailTemplate.EMAIL_CHANGED, variables, oldEmail);
  }

  private void sendEmail(String to, String subject, EmailTemplate template,
      Map<String, String> variables) {
    sendEmail(to, subject, template, variables, null);
  }

  /**
   * Send an email message.
   *
   * @param to The target email address.
   * @param subject The message subject.
   * @param template The email template.
   * @param variables The template variables.
   * @param carbonCopy The carbon copy email address, if any.
   */
  private void sendEmail(String to, String subject, EmailTemplate template,
      Map<String, String> variables, String carbonCopy) {
    String from = "TaskNote App <" + senderEmail + ">";
    String html = template.render(variables);

    ResendEmailRequest emailRequest =
        new ResendEmailRequest(
            from,
            List.of(to),
            carbonCopy != null ? List.of(carbonCopy) : null,
            subject,
            html);

    try {
      restClient
          .post()
          .uri("/emails")
          .contentType(MediaType.APPLICATION_JSON)
          .body(emailRequest)
          .retrieve()
          .toBodilessEntity();

      logger.info("Email message send successfully.");
    } catch (HttpClientErrorException ex) {
      logger.error("Unable to send email: {} - {}", ex.getMessage(), ex.getStatusCode());
    } catch (Exception ex) {
      logger.error("Unexpected error sending email: {}", ex.getMessage());
    }
  }

  private String getBaseUrl() {
    if ("development".equals(targetEnv) || Objects.isNull(targetEnv)) {
      return "http://localhost:5000";
    }
    String baseUrl = domain;
    if (targetEnv.equals("staging")) {
      baseUrl = "tasknote-stg" + domain.substring(8);
    }
    return String.format("https://%s", baseUrl);
  }
}
