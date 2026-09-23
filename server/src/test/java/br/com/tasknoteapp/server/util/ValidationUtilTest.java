package br.com.tasknoteapp.server.util;

import br.com.tasknoteapp.server.exception.InternalValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationUtilTest {

  @Test
  void notNullNorBlank_nullValue_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.notNullNorBlank("name", null);
        });

    Assertions.assertEquals("name", ex.getErrorKey());
    Assertions.assertEquals("must not be null or empty", ex.getMessage());
  }

  @Test
  void notNullNorBlank_emptyValue_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.notNullNorBlank("name", "");
        });

    Assertions.assertEquals("name", ex.getErrorKey());
    Assertions.assertEquals("must not be null or empty", ex.getMessage());
  }

  @Test
  void notNullNorBlank_blankValue_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.notNullNorBlank("name", "   ");
        });

    Assertions.assertEquals("name", ex.getErrorKey());
    Assertions.assertEquals("must not be null or empty", ex.getMessage());
  }

  @Test
  void notNullNorBlank_validValue_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.notNullNorBlank("name", "John");
    });
  }

  @Test
  void email_validEmail_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.email("email", "user.name+tag@example.com");
    });
  }

  @Test
  void email_missingAtSymbol_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.email("email", "user.example.com");
        });

    Assertions.assertEquals("email", ex.getErrorKey());
    Assertions.assertEquals("must be a well-formed email address", ex.getMessage());
  }

  @Test
  void email_missingDomain_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.email("email", "user@");
        });

    Assertions.assertEquals("email", ex.getErrorKey());
    Assertions.assertEquals("must be a well-formed email address", ex.getMessage());
  }

  @Test
  void email_missingTld_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.email("email", "user@example.");
        });

    Assertions.assertEquals("email", ex.getErrorKey());
    Assertions.assertEquals("must be a well-formed email address", ex.getMessage());
  }

  @Test
  void email_singleCharTld_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.email("email", "user@example.c");
        });

    Assertions.assertEquals("email", ex.getErrorKey());
    Assertions.assertEquals("must be a well-formed email address", ex.getMessage());
  }

  @Test
  void maxSize_valueWithinLimit_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.maxSize("title", "short", 10);
    });
  }

  @Test
  void maxSize_valueAtLimit_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.maxSize("title", "1234567890", 10);
    });
  }

  @Test
  void maxSize_valueAboveLimit_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.maxSize("title", "12345678901", 10);
        });

    Assertions.assertEquals("title", ex.getErrorKey());
    Assertions.assertEquals("size must be between 0 and 10", ex.getMessage());
  }

  @Test
  void url_validHttpUrl_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.url("url", "http://example.com/page");
    });
  }

  @Test
  void url_validHttpsUrl_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.url("url", "https://example.com/page?x=1");
    });
  }

  @Test
  void url_fragmentUrl_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.url("url", "#section");
    });
  }

  @Test
  void url_emptyValue_returnsEmptyMapTest() {
    Assertions.assertDoesNotThrow(() -> {
      ValidationUtil.url("url", "");
    });
  }

  @Test
  void url_invalidUrl_returnsErrorTest() {
    InternalValidationException ex = 
        Assertions.assertThrows(InternalValidationException.class, () -> {
          ValidationUtil.url("url", "example.com");
        });

    Assertions.assertEquals("url", ex.getErrorKey());
    Assertions.assertEquals("must be a well-formed url address", ex.getMessage());
  }
}
