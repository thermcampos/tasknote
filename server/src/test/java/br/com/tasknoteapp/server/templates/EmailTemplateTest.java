package br.com.tasknoteapp.server.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Test class for EmailTemplate. */
class EmailTemplateTest {

  @Test
  void renderSignUpTemplate_shouldReplaceConfirmationLink() {
    String link = "http://localhost:5000/email-confirmation?identification=abc-123";

    String html = EmailTemplate.SIGN_UP.render(Map.of("CONFIRMATION_LINK", link));

    assertTrue(html.contains(link));
    assertFalse(html.contains("{{"));
  }

  @Test
  void renderPasswordResetTemplate_shouldReplaceResetLink() {
    String link = "http://localhost:5000/finish-reset-password?token=token-123";

    String html = EmailTemplate.PASSWORD_RESET.render(Map.of("RESET_LINK", link));

    assertTrue(html.contains(link));
    assertFalse(html.contains("{{"));
  }

  @Test
  void renderPasswordResetConfirmTemplate_withoutVariables_shouldReturnHtml() {
    String html = EmailTemplate.PASSWORD_RESET_CONFIRM.render(Map.of());

    assertTrue(html.contains("<html"));
    assertFalse(html.contains("{{"));
  }

  @Test
  void renderEmailChangedTemplate_shouldReplaceBothEmails() {
    String html =
        EmailTemplate.EMAIL_CHANGED.render(
            Map.of("EMAIL_FROM", "old@example.com", "EMAIL_TO", "new@example.com"));

    assertTrue(html.contains("old@example.com"));
    assertTrue(html.contains("new@example.com"));
    assertFalse(html.contains("{{"));
  }

  @Test
  void render_withValueContainingDollarSign_shouldNotBreakReplacement() {
    String html = EmailTemplate.PASSWORD_RESET.render(Map.of("RESET_LINK", "http://x/$1/reset"));

    assertTrue(html.contains("http://x/$1/reset"));
    assertEquals(-1, html.indexOf("{{ RESET_LINK }}"));
  }
}
