package br.com.tasknoteapp.server.util;

import java.util.Map;
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
   * @return Map of String String containing an ERROR_KEY key if present, or empty.
   */
  public static Map<String, String> notNullNorBlank(String field, String value) {
    if (Objects.isNull(value) || value.isBlank()) {
      return Map.of(
          ERROR_KEY, field,
          field, "must not be null or empty"
      );
    }
    return Map.of();
  }

  /**
   * Validates a field against an email regular expression pattern.
   *
   * @param field The field name to be added to the response error.
   * @param value The email to be validated.
   * @return Map of tring String containing an ERROR_KEY key if present, or empty.
   */
  public static Map<String, String> email(String field, String value) {
    Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    if (!emailPattern.matcher(value).matches()) {
      return Map.of(
          ERROR_KEY, field,
          field, "must be a well-formed email address"
      );
    }
    return Map.of();
  }

  /**
   * Validates a field length against a max size value.
   *
   * @param field The field name to be added to the response error.
   * @param value The field value to be validated.
   * @param maxSize The integer max size to be validated against.
   * @return Map of tring String containing an ERROR_KEY key if present, or empty.
   */
  public static Map<String, String> maxSize(String field, String value, int maxSize) {
    if (value.length() > maxSize) {
      return Map.of(
        ERROR_KEY, field,
        field, "size must be between 0 and " + maxSize
      );
    }
    return Map.of();
  }

  /**
   * Validates a field against an URL regular expression pattern.
   *
   * @param field The field name to be added to the response error.
   * @param value The url to be validated.
   * @return Map of tring String containing an ERROR_KEY key if present, or empty.
   */
  public static Map<String, String> url(String field, String value) {
    Pattern urlPattern = Pattern.compile("^(https?://.*|#.*)?$");
    if (!urlPattern.matcher(value).matches()) {
      return Map.of(
          ERROR_KEY, field,
          field, "must be a well-formed url address"
      );
    }
    return Map.of();
  }
}
