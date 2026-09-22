package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@RepositoryIntTest
@Sql(scripts = {"classpath:sql/UserRepositoryTest.sql"})
class UserRepositoryIntTest {

  @Autowired UserRepository userRepository;

  private static final String UUID_CODE = "cc2b5506-83ed-5764-985e-611ad4ce8050";

  @Test
  void findByEmailUuid_happyPath_shouldSucceed() {
    UUID uuid = UUID.fromString(UUID_CODE);
    Optional<User> user = userRepository.findByEmailUuid(uuid);

    Assertions.assertFalse(user.isEmpty());
  }

  @Test
  void findByResetToken_happyPath_shouldSucceed() {
    String token = "abc123456";
    Optional<User> user = userRepository.findByResetToken(token);

    Assertions.assertFalse(user.isEmpty());
  }
}
