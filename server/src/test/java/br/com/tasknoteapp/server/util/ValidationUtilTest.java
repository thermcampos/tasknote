package br.com.tasknoteapp.server.util;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationUtilTest {

  @Test
  void notNullNorBlank_nullValue_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.notNullNorBlank("name", null);

    Assertions.assertEquals("name", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must not be null or empty", result.get("name"));
  }

  @Test
  void notNullNorBlank_emptyValue_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.notNullNorBlank("name", "");

    Assertions.assertEquals("name", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must not be null or empty", result.get("name"));
  }

  @Test
  void notNullNorBlank_blankValue_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.notNullNorBlank("name", "   ");

    Assertions.assertEquals("name", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must not be null or empty", result.get("name"));
  }

  @Test
  void notNullNorBlank_validValue_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.notNullNorBlank("name", "John");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void email_validEmail_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.email("email", "user.name+tag@example.com");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void email_missingAtSymbol_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.email("email", "user.example.com");

    Assertions.assertEquals("email", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must be a well-formed email address", result.get("email"));
  }

  @Test
  void email_missingDomain_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.email("email", "user@");

    Assertions.assertEquals("email", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must be a well-formed email address", result.get("email"));
  }

  @Test
  void email_missingTld_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.email("email", "user@example.");

    Assertions.assertEquals("email", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must be a well-formed email address", result.get("email"));
  }

  @Test
  void email_singleCharTld_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.email("email", "user@example.c");

    Assertions.assertEquals("email", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must be a well-formed email address", result.get("email"));
  }

  @Test
  void maxSize_valueWithinLimit_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.maxSize("title", "short", 10);

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void maxSize_valueAtLimit_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.maxSize("title", "1234567890", 10);

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void maxSize_valueAboveLimit_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.maxSize("title", "12345678901", 10);

    Assertions.assertEquals("title", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("size must be between 0 and 10", result.get("title"));
  }

  @Test
  void url_validHttpUrl_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.url("url", "http://example.com/page");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void url_validHttpsUrl_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.url("url", "https://example.com/page?x=1");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void url_fragmentUrl_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.url("url", "#section");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void url_emptyValue_returnsEmptyMapTest() {
    Map<String, String> result = ValidationUtil.url("url", "");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void url_invalidUrl_returnsErrorTest() {
    Map<String, String> result = ValidationUtil.url("url", "example.com");

    Assertions.assertEquals("url", result.get(ValidationUtil.ERROR_KEY));
    Assertions.assertEquals("must be a well-formed url address", result.get("url"));
  }
}
