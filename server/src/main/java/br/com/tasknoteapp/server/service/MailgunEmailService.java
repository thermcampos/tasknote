package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.templates.MailgunTemplate;
import br.com.tasknoteapp.server.templates.MailgunTemplateEmailChanged;
import br.com.tasknoteapp.server.templates.MailgunTemplateResetPwd;
import br.com.tasknoteapp.server.templates.MailgunTemplateResetPwdConfirm;
import br.com.tasknoteapp.server.templates.MailgunTemplateSignUp;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** This service handles email messages for Mailgun. */
@Service
public class MailgunEmailService {

  private static final Logger logger = LoggerFactory.getLogger(MailgunEmailService.class.getName());
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
  public MailgunEmailService(
      @Value("${mailgun.api-key}") String apiKey,
      @Value("${mailgun.domain}") String domain,
      @Value("${mailgun.sender-email}") String sender,
      @Value("${br.com.tasknote.server.target-env}") String targetEnv,
      RestClient.Builder restClientBuilder) {
    this.domain = domain;
    this.senderEmail = sender;
    this.targetEnv = targetEnv;

    if (apiKey != null && apiKey.length() > 6) {
      logger.info(
          "Mailgun API Key loaded: {}...{}",
          apiKey.substring(0, 3),
          apiKey.substring(apiKey.length() - 3));
    } else {
      logger.warn("Mailgun API Key is missing or too short!");
    }

    this.restClient =
        restClientBuilder
            .baseUrl("https://api.mailgun.net/v3/" + domain)
            .defaultStatusHandler(
                HttpStatusCode::isError,
                (request, response) ->
                    logger.error(
                        "Mailgun API Error: {} {}",
                        response.getStatusCode(),
                        response.getStatusText()))
            .defaultHeaders(headers -> headers.setBasicAuth("api", apiKey))
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

    MailgunTemplateSignUp signUpTemplate = new MailgunTemplateSignUp();
    signUpTemplate.setConfirmationLink(String.format(link, user.getEmailUuid().toString()));

    sendEmail(to, subject, signUpTemplate);
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

    MailgunTemplateResetPwd resetTemplate = new MailgunTemplateResetPwd();
    resetTemplate.setResetLink(String.format(link, user.getResetToken()));

    sendEmail(to, subject, resetTemplate);
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

    MailgunTemplateResetPwdConfirm resetTemplate = new MailgunTemplateResetPwdConfirm();

    sendEmail(to, subject, resetTemplate);
  }

  /**
   * Send a notification about the changed email for both previous and new email.
   *
   * @param user The user that should be addressed the message.
   * @param oldEmail The user previous email
   */
  public void sendEmailChangedNotification(User user, String oldEmail) {
    logger.info("Sending message with changed email notification");

    MailgunTemplateEmailChanged emailChanged = new MailgunTemplateEmailChanged();
    emailChanged.setEmailFrom(oldEmail);
    emailChanged.setEmailTo(user.getEmail());
    emailChanged.setCarbonCopy(oldEmail);

    String subject = "TaskNote App email changed notification";

    sendEmail(user.getEmail(), subject, emailChanged);
  }

  /**
   * Send an email message.
   *
   * @param to The target email address.
   * @param subject The message subject.
   * @param template The Mailgun template.
   */
  private void sendEmail(String to, String subject, MailgunTemplate template) {
    String from = "TaskNote App <" + senderEmail + ">";

    MultiValueMap<String, String> mailData = new LinkedMultiValueMap<>();
    mailData.add("from", from);
    mailData.add("to", to);
    if (template.getCarbonCopy().isPresent()) {
      mailData.add("cc", template.getCarbonCopy().get());
    }
    mailData.add("subject", subject);
    mailData.add("template", template.getName());
    if (!template.getVariables().isEmpty()) {
      mailData.add("h:X-Mailgun-Variables", template.getVariableValuesJson());
      logger.info("JSON template variables: {}", template.getVariableValuesJson());
    }

    try {
      restClient
          .post()
          .uri("/messages")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(mailData)
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
