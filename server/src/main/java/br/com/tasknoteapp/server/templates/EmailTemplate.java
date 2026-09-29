package br.com.tasknoteapp.server.templates;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/** This enum represents the available email templates backed by local HTML files. */
public enum EmailTemplate {
  SIGN_UP("sign_up_confirmation.html"),
  PASSWORD_RESET("password_reset.html"),
  PASSWORD_RESET_CONFIRM("password_change_confirmation.html"),
  EMAIL_CHANGED("email_changed.html");

  private static final String TEMPLATES_FOLDER = "email-templates/";
  private static final String PLACEHOLDER_REGEX = "\\{\\{\\s*%s\\s*\\}\\}";

  private final String fileName;

  EmailTemplate(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Render the template HTML replacing the {@code {{ VARIABLE }}} placeholders.
   *
   * @param variables The values to replace the template placeholders with.
   * @return The rendered HTML content.
   */
  public String render(Map<String, String> variables) {
    String html = loadTemplate();
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String placeholder = String.format(PLACEHOLDER_REGEX, Pattern.quote(entry.getKey()));
      html = html.replaceAll(placeholder, Matcher.quoteReplacement(entry.getValue()));
    }
    return html;
  }

  private String loadTemplate() {
    try {
      return new ClassPathResource(TEMPLATES_FOLDER + fileName)
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException("Unable to load email template " + fileName, ex);
    }
  }
}
