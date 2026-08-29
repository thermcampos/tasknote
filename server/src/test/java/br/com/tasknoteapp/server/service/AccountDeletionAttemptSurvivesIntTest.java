package br.com.tasknoteapp.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.entity.UserPwdLimitEntity;
import br.com.tasknoteapp.server.exception.InvalidCredentialsException;
import br.com.tasknoteapp.server.repository.UserPwdLimitRepository;
import br.com.tasknoteapp.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies the failed-attempt record survives the account-deletion rollback. Runs in its own test
 * class (non-transactional, committed data) so it does not share a persistence context with the
 * transactional {@link AccountDeletionIntTest}.
 */
@SpringBootTest
class AccountDeletionAttemptSurvivesIntTest {

  private static final String RAW_PASSWORD = "a1b2c3d4f5g6";
  private static final String EMAIL = "account-deletion-survives@domain.com";

  @Autowired private UserSessionService userSessionService;

  @Autowired private AuthService authService;

  @Autowired private UserRepository userRepository;

  @Autowired private UserPwdLimitRepository userPwdLimitRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private TransactionTemplate transactionTemplate;

  private UserEntity user;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        tx ->
            userRepository
                .findByEmail(EMAIL)
                .ifPresent(
                    u -> {
                      userPwdLimitRepository.deleteAllForUser(u.getId());
                      userRepository.deleteById(u.getId());
                    }));

    user = new UserEntity();
    user.setEmail(EMAIL);
    user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
    user.setAdmin(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setLastPasswordChange(LocalDateTime.now());
    user = transactionTemplate.execute(tx -> userRepository.save(user));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    transactionTemplate.executeWithoutResult(
        tx ->
            userRepository
                .findByEmail(EMAIL)
                .ifPresent(
                    u -> {
                      userPwdLimitRepository.deleteAllForUser(u.getId());
                      userRepository.deleteById(u.getId());
                    }));
  }

  @Test
  @DisplayName("Failed attempt row should survive when no surrounding transaction holds it")
  void deleteAccount_wrongPassword_attemptShouldSurvive() {
    Long userId = user.getId();

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.verifyCurrentPassword(user, "wrong-password");
          userSessionService.deleteCurrentUserAccount();
        });

    assertTrue(userRepository.findById(userId).isPresent());

    List<UserPwdLimitEntity> attempts =
        userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(userId);
    assertEquals(1, attempts.size());
  }
}
