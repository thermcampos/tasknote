package br.com.tasknoteapp.server.util;

import br.com.tasknoteapp.server.exception.InternalValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

/** This class contains methods to validate inputs from JSON parsing. */
public class ValidationUtil {

  public static final String ERROR_KEY = "error";
  public static final Integer MAX_EMAIL_SIZE = 100;
  public static final Integer MAX_URL_SIZE = 180;
  public static final Integer MAX_PASSWORD_SIZE = 30;
  public static final Integer MAX_LANG_SIZE = 2;
  public static final Integer MAX_NOTE_TITLE_SIZE = 100;
  public static final Integer MAX_NOTE_CONTENT_SIZE = 50000;
  public static final Integer MAX_TASK_NAME = 180;
  public static final Integer MAX_TASK_DUEDATE = 10;
  public static final Integer MAX_TAG_NAME_SIZE = 20;
  public static final Integer MAX_TOKEN_SIZE = 32;
  public static final Integer MAX_UUID_SIZE = 36;
  
  /**
   * Validates a field agains being null, empty or blank.
   *
   * @param field The field name to be added to the response error.
   * @param value The field value to be validated.
   * @throws InternalValidationException if validation fails.
   */
  public static void notNullNorBlank(String field, String value) {
    if (Objects.isNull(value) || value.isBlank()) {
      throw new InternalValidationException(field, "must not be null or empty");
    }
  }

  /**
   * Validates a field against an email regular expression pattern.
   *
   * @param field The field name to be added to the response error.
   * @param value The email to be validated.
   * @throws InternalValidationException if validation fails.
   */
  public static void email(String field, String value) {
    Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    if (!emailPattern.matcher(value).matches()) {
      throw new InternalValidationException(field, "must be a well-formed email address");
    }
  }

  /**
   * Validates a field length against a max size value.
   *
   * @param field The field name to be added to the response error.
   * @param value The field value to be validated.
   * @param maxSize The integer max size to be validated against.
   * @throws InternalValidationException if validation fails.
   */
  public static void maxSize(String field, String value, int maxSize) {
    if (value.length() > maxSize) {
      throw new InternalValidationException(field, "size must be between 0 and " + maxSize);
    }
  }

  /**
   * Validates a field against an URL regular expression pattern.
   *
   * @param field The field name to be added to the response error.
   * @param value The url to be validated.
   * @throws InternalValidationException if validation fails.
   */
  public static void url(String field, String value) {
    Pattern urlPattern = Pattern.compile("^(https?://.*|#.*)?$");
    if (!urlPattern.matcher(value).matches()) {
      throw new InternalValidationException(field, "must be a well-formed url address");
    }
  }

  /**
   * Validates a field against a LocalDate parser for the format YYYY-MM-DD.
   *
   * @param field The field name to be added to the response error.
   * @param value The date to be validated.
   * @throws InternalValidationException if validation fails.
   */
  public static void date(String field, String value) {
    try {
      LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new InternalValidationException(field, "must be a valid format");
    }
  }
}
