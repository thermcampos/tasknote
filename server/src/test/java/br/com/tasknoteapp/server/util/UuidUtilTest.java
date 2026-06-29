package br.com.tasknoteapp.server.util;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UuidUtilTest {

  @Test
  void generateEmailUuidTest() {
    UuidUtil uuidUtil = new UuidUtil();
    String email = "example@test.com";
    UUID uuid = uuidUtil.generateEmailUuid(email);

    Assertions.assertNotNull(uuid);
    Assertions.assertNotNull(uuidUtil.generateEmailUuid(email));
  }
}
