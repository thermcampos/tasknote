package br.com.tasknoteapp.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.entity.UserPwdLimitEntity;
import br.com.tasknoteapp.server.exception.BadPasswordException;
import br.com.tasknoteapp.server.exception.BadThemeException;
import br.com.tasknoteapp.server.exception.BadUuidException;
import br.com.tasknoteapp.server.exception.EmailAlreadyExistsException;
import br.com.tasknoteapp.server.exception.EmailNotConfirmedException;
import br.com.tasknoteapp.server.exception.InvalidCredentialsException;
import br.com.tasknoteapp.server.exception.MaxLoginLimitAttemptException;
import br.com.tasknoteapp.server.exception.ResetExpiredException;
import br.com.tasknoteapp.server.exception.UserForbiddenException;
import br.com.tasknoteapp.server.exception.UserNotFoundException;
import br.com.tasknoteapp.server.repository.UserPwdLimitRepository;
import br.com.tasknoteapp.server.repository.UserRepository;
import br.com.tasknoteapp.server.request.LoginRequest;
import br.com.tasknoteapp.server.request.PasswordResetRequest;
import br.com.tasknoteapp.server.request.UserPatchRequest;
import br.com.tasknoteapp.server.response.UserResponse;
import br.com.tasknoteapp.server.response.UserResponseWithToken;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  @Mock private AuthenticationManager authenticationManager;

  @Mock private AuthUtil authUtil;

  @Mock private UserPwdLimitRepository userPwdLimitRepository;

  @Mock private MailgunEmailService mailgunEmailService;

  @Mock private Environment environment;

  private AuthService authService;

  @BeforeEach
  void setup() {
    authService =
        new AuthService(
            userRepository,
            passwordEncoder,
            jwtService,
            authenticationManager,
            authUtil,
            userPwdLimitRepository,
            mailgunEmailService,
            environment);
  }

  @Test
  @DisplayName("SignUp new user happy path should succeed")
  void signUpNewUser_happyPath_shouldSucceed() {
    LoginRequest request =
        new LoginRequest("email@domain.com", "123456@abcde!", "123456@abcde!", "en");

    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(authUtil.validatePassword(request.password())).thenReturn(Optional.empty());

    UserEntity entity = new UserEntity();
    entity.setId(3L);
    entity.setEmail(request.email());
    entity.setName("User");
    entity.setEmailUuid(UUID.randomUUID());

    when(userRepository.save(any())).thenReturn(entity);

    UserResponseWithToken token = authService.signUpNewUser(request);

    Assertions.assertNotNull(token);
    Assertions.assertNull(token.token());
    Assertions.assertEquals(entity.getEmail(), token.email());
    Assertions.assertNotNull(entity.getEmailUuid());
    verify(userRepository).save(any(UserEntity.class));
  }

  @Test
  @DisplayName("SignUp new user with existing email should fail")
  void signUpNewUser_emailExists_shouldFail() {
    LoginRequest request =
        new LoginRequest("email@domain.com", "123456@abcde!", "123456@abcde!", "en");

    UserEntity existing = new UserEntity();
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existing));

    Assertions.assertThrows(
        EmailAlreadyExistsException.class,
        () -> {
          authService.signUpNewUser(request);
        });
  }

  @Test
  @DisplayName("SignUp new user with bad password should fail")
  void signUpNewUser_badPassword_shouldFail() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");

    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(authUtil.validatePassword(request.password())).thenReturn(Optional.of("Bad password"));

    Assertions.assertThrows(
        BadPasswordException.class,
        () -> {
          authService.signUpNewUser(request);
        });
  }

  @Test
  @DisplayName("Find user by email happy path should succeed")
  void findByEmail_happyPath_shouldSucceed() {
    String email = "user@email.com";

    UserEntity existing = new UserEntity();
    existing.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    Optional<UserEntity> userOp = authService.findByEmail(email);

    Assertions.assertTrue(userOp.isPresent());
    Assertions.assertEquals(email, userOp.get().getEmail());
  }

  @Test
  @DisplayName("Find user by email not found should succeed")
  void findByEmail_notFound_shouldSucceed() {
    String email = "user@email.com";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Optional<UserEntity> userOp = authService.findByEmail(email);

    Assertions.assertTrue(userOp.isEmpty());
  }

  @Test
  @DisplayName("Load user by username happy path should succeed")
  void loadUserByUsername_happyPath_shouldSucceed() {
    String email = "user@domain.com";

    UserEntity existing = new UserEntity();
    existing.setEmail(email);
    existing.setPassword(email + "123");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    User user = authService.loadUserByUsername(email);

    Assertions.assertNotNull(user);
    Assertions.assertEquals(email, user.getUsername());
  }

  @Test
  @DisplayName("Load user by username not found should fail")
  void loadUserByUsername_notFound_shouldFail() {
    String email = "user@domain.com";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class,
        () -> {
          authService.loadUserByUsername(email);
        });
  }

  @Test
  @DisplayName("SignIn user happy path should succeed")
  void signInUser_happyPath_shouldSucceed() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");
    LocalDateTime oldLastLogin = LocalDateTime.of(2026, 5, 1, 10, 20, 30);

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(request.email());
    existing.setEmailConfirmedAt(LocalDateTime.now());
    existing.setLastLogin(oldLastLogin);
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existing));

    when(userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(existing.getId()))
        .thenReturn(List.of());
    when(authenticationManager.authenticate(any())).thenReturn(null);
    when(jwtService.generateToken(existing)).thenReturn("a1b2c3");

    doNothing().when(userPwdLimitRepository).deleteAllForUser(existing.getId());

    UserResponseWithToken token = authService.signInUser(request);

    Assertions.assertNotNull(token);
    Assertions.assertEquals("a1b2c3", token.token());
    Assertions.assertEquals(oldLastLogin, token.lastLogin());
  }

  @Test
  @DisplayName("SignIn user not confirmed should fail")
  void signInUser_notConfirmed_shouldFail() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(request.email());
    existing.setEmailConfirmedAt(null);
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existing));

    when(userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(existing.getId()))
        .thenReturn(List.of());

    Assertions.assertThrows(
        EmailNotConfirmedException.class,
        () -> {
          authService.signInUser(request);
        });
  }

  @Test
  @DisplayName("SignIn wrong user or password should fail")
  void signInUser_wrongUserOrPassword_shouldFail() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");

    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

    Assertions.assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.signInUser(request);
        });
  }

  @Test
  @DisplayName("SignIn max login attempt should fail")
  void signInUser_maxLoginAttempt_shouldFail() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmailConfirmedAt(LocalDateTime.now());
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existing));

    UserPwdLimitEntity limit1 = new UserPwdLimitEntity();
    limit1.setWhenHappened(LocalDateTime.now().minusSeconds(30));
    UserPwdLimitEntity limit2 = new UserPwdLimitEntity();
    limit2.setWhenHappened(LocalDateTime.now().minusMinutes(1));
    UserPwdLimitEntity limit3 = new UserPwdLimitEntity();
    limit3.setWhenHappened(LocalDateTime.now().minusMinutes(2));
    when(userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(existing.getId()))
        .thenReturn(List.of(limit1, limit2, limit3));

    Assertions.assertThrows(
        MaxLoginLimitAttemptException.class,
        () -> {
          authService.signInUser(request);
        });
  }

  @Test
  @DisplayName("SignIn bad credentials should fail")
  void signInUser_badCredentials_shouldFail() {
    LoginRequest request = new LoginRequest("email@domain.com", "123456", "123456", "en");

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmailConfirmedAt(LocalDateTime.now());
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existing));

    when(userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(existing.getId()))
        .thenReturn(List.of());
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Wrong"));

    UserResponseWithToken token = authService.signInUser(request);

    Assertions.assertNull(token);
    verify(userPwdLimitRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("Get all users happy path should succeed")
  void getAllUsers_happyPath_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(true);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    UserEntity user1 = new UserEntity();
    user1.setEmail("user1@domain.com");
    UserEntity user2 = new UserEntity();
    user2.setEmail("user2@domain.com");
    when(userRepository.findAll()).thenReturn(List.of(user1, user2));

    List<UserResponse> users = authService.getAllUsers();

    Assertions.assertNotNull(users);
    Assertions.assertFalse(users.isEmpty());
    Assertions.assertEquals(user1.getEmail(), users.get(0).email());
    Assertions.assertEquals(user2.getEmail(), users.get(1).email());
  }

  @Test
  @DisplayName("Get all users no current user should fail")
  void getAllUsers_noCurrentUser_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class,
        () -> {
          authService.getAllUsers();
        });
  }

  @Test
  @DisplayName("Get all users user not found should fail")
  void getAllUsers_userNotFound_shouldFail() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class,
        () -> {
          authService.getAllUsers();
        });
  }

  @Test
  @DisplayName("Get all users user not admin should fail")
  void getAllUsers_userNotAdmin_shouldFail() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    Assertions.assertThrows(
        UserForbiddenException.class,
        () -> {
          authService.getAllUsers();
        });
  }

  @Test
  @DisplayName("Refresh current user token happy path should succeed")
  void refreshCurrentUserToken_happyPath_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    when(jwtService.generateToken(existing)).thenReturn("a1b2c3");

    String token = authService.refreshCurrentUserToken();

    Assertions.assertNotNull(token);
    Assertions.assertEquals("a1b2c3", token);
  }

  @Test
  @DisplayName("Delete user account happy path should succeed")
  void deleteUserAccount_happyPath_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
    doNothing().when(userPwdLimitRepository).deleteAllForUser(existing.getId());
    doNothing().when(userRepository).delete(existing);

    UserResponse response = authService.deleteUserAccount();

    Assertions.assertNotNull(response);
  }

  @Test
  @DisplayName("Patch user info patch user name and email should succeed")
  void pathUserInfo_nameAndEmail_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setName(null);
    existing.setEmail(email);
    existing.setAdmin(false);
    existing.setPassword("hashedCurrentPassword");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
    when(userRepository.save(any())).thenReturn(existing);

    String currentPassword = "currentPw123@";
    when(passwordEncoder.matches(currentPassword, "hashedCurrentPassword")).thenReturn(true);

    UserPatchRequest patchRequest =
        new UserPatchRequest(
            "Kong", "newemail@domain.com", null, null, null, null, currentPassword);
    UserResponse response = authService.patchUserInfo(patchRequest);

    Assertions.assertNotNull(response);
    Assertions.assertEquals("Kong", response.name());
    Assertions.assertEquals("newemail@domain.com", response.email());
  }

  @Test
  @DisplayName("Patch user info patch user password should succeed")
  void pathUserInfo_password_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setName(null);
    existing.setEmail(email);
    existing.setAdmin(false);
    existing.setPassword("hashedCurrentPassword");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
    when(userRepository.save(any())).thenReturn(existing);

    String currentPassword = "currentPw123@";
    when(passwordEncoder.matches(currentPassword, "hashedCurrentPassword")).thenReturn(true);

    String newPassword = "TestHackedPw@difficult!#:)";
    UserPatchRequest patchRequest =
        new UserPatchRequest(
            "Kong", "newemail@domain.com", newPassword, newPassword, "en", null, currentPassword);

    when(authUtil.validatePassword(patchRequest.password())).thenReturn(Optional.empty());

    UserResponse response = authService.patchUserInfo(patchRequest);

    Assertions.assertNotNull(response);
    Assertions.assertEquals("Kong", response.name());
    Assertions.assertEquals("newemail@domain.com", response.email());
  }

  @Test
  @DisplayName("Patch user info patch user theme should succeed")
  void patchUserInfo_theme_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
    when(userRepository.save(any())).thenReturn(existing);

    UserPatchRequest patchRequest =
        new UserPatchRequest(null, null, null, null, null, "dark", null);
    UserResponse response = authService.patchUserInfo(patchRequest);

    Assertions.assertNotNull(response);
    Assertions.assertEquals("dark", response.theme());
    Assertions.assertEquals("dark", existing.getTheme());
    verify(userRepository, times(1)).save(existing);
  }

  @Test
  @DisplayName("Patch user info with invalid theme should fail")
  void patchUserInfo_invalidTheme_shouldFail() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    UserPatchRequest patchRequest =
        new UserPatchRequest(null, null, null, null, null, "blue", null);

    Assertions.assertThrows(
        BadThemeException.class, () -> authService.patchUserInfo(patchRequest));
    verify(userRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("Patch user info with blank theme should ignore it")
  void patchUserInfo_blankTheme_shouldBeIgnored() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    UserPatchRequest patchRequest =
        new UserPatchRequest(null, null, null, null, null, " ", null);
    UserResponse response = authService.patchUserInfo(patchRequest);

    Assertions.assertNotNull(response);
    Assertions.assertEquals("light", response.theme());
    Assertions.assertNull(existing.getTheme());
    verify(userRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("Get the current user happy path should succeed")
  void getCurrentUser_happyPath_shouldSucceed() {
    String email = "user@domain.com";
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of(email));

    UserEntity existing = new UserEntity();
    existing.setId(775L);
    existing.setName("Test");
    existing.setEmail(email);
    existing.setAdmin(false);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    Optional<UserEntity> userOptional = authService.getCurrentUser();

    Assertions.assertNotNull(userOptional);
    Assertions.assertTrue(userOptional.isPresent());
  }

  @Test
  @DisplayName("Get the current user with user not found it should fail")
  void getCurrentUser_userNotFound_shouldFail() {
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.empty());

    Assertions.assertThrows(UserNotFoundException.class, () -> authService.getCurrentUser());
  }

  @Test
  @DisplayName("Confirm user account happy path should succeed")
  void confirmUserAccount_happyPath_shouldSucceed() {
    String uuid = UUID.randomUUID().toString();

    UserEntity user = new UserEntity();
    user.setEmailUuid(UUID.fromString(uuid));
    when(userRepository.findByEmailUuid(UUID.fromString(uuid))).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenReturn(user);

    Assertions.assertDoesNotThrow(() -> authService.confirmUserAccount(uuid));
    verify(userRepository, times(1)).save(user);
    Assertions.assertNotNull(user.getEmailConfirmedAt());
  }

  @Test
  @DisplayName("Confirm user account with invalid UUID should fail")
  void confirmUserAccount_invalidUuid_shouldFail() {
    String invalidUuid = "invalid-uuid";

    Assertions.assertThrows(
        BadUuidException.class,
        () -> {
          authService.confirmUserAccount(invalidUuid);
        });
    verify(userRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("Confirm user account with non-existent UUID should fail")
  void confirmUserAccount_nonExistentUuid_shouldFail() {
    String uuid = UUID.randomUUID().toString();

    when(userRepository.findByEmailUuid(UUID.fromString(uuid))).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class,
        () -> {
          authService.confirmUserAccount(uuid);
        });
    verify(userRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("Resend email confirmation happy path should succeed")
  void resendEmailConfirmation_happyPath_shouldSucceed() {
    String email = "user@domain.com";

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
    when(environment.getProperty("MAILGUN_APIKEY")).thenReturn("abc");

    doNothing().when(mailgunEmailService).sendNewUser(existing);

    Assertions.assertDoesNotThrow(() -> authService.resendEmailConfirmation(email));
    verify(mailgunEmailService, times(1)).sendNewUser(existing);
  }

  @Test
  @DisplayName("Resend email confirmation no mailgun api key should succeed")
  void resendEmailConfirmation_noMailgunApiKey_shouldSucceed() {
    String email = "user@domain.com";

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    Assertions.assertDoesNotThrow(() -> authService.resendEmailConfirmation(email));
    verify(mailgunEmailService, times(0)).sendNewUser(existing);
  }

  @Test
  @DisplayName("Resend email confirmation with non-existent email should fail")
  void resendEmailConfirmation_nonExistentEmail_shouldFail() {
    String email = "nonexistent@domain.com";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class, () -> authService.resendEmailConfirmation(email));
    verify(mailgunEmailService, times(0)).sendNewUser(any());
  }

  @Test
  @DisplayName("Reset password for user happy path should succeed")
  void resetPasswordForUser_happyPath_shouldSucceed() {
    String email = "user@domain.com";

    UserEntity existing = new UserEntity();
    existing.setId(919L);
    existing.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

    doNothing().when(mailgunEmailService).sendResetPassword(any());
    when(userRepository.save(any())).thenReturn(existing);
    when(environment.getProperty("MAILGUN_APIKEY")).thenReturn("abc");

    Assertions.assertDoesNotThrow(() -> authService.resetPasswordForUser(email));
    verify(userRepository, times(1)).save(existing);
    verify(mailgunEmailService, times(1)).sendResetPassword(existing);
  }

  @Test
  @DisplayName("Reset password for user with non-existent email should succeed without exception")
  void resetPasswordForUser_nonExistentEmail_shouldSucceed() {
    String email = "nonexistent@domain.com";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertDoesNotThrow(() -> authService.resetPasswordForUser(email));
    verify(userRepository, times(0)).save(any());
    verify(mailgunEmailService, times(0)).sendResetPassword(any());
  }

  @Test
  @DisplayName("Confirm reset password happy path should succeed")
  void confirmResetPasswordForUser_happyPath_shouldSucceed() {
    String token = "validToken";
    UserEntity user = new UserEntity();
    user.setResetToken(token);
    user.setResetPasswordExpiration(LocalDateTime.now().plusMinutes(30));

    String newPassword = "NewPassword@123";

    String encodedPassword = "hash@abcxasd123!";
    when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
    user.setPassword(encodedPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
    when(authUtil.validatePassword(newPassword)).thenReturn(Optional.empty());
    when(environment.getProperty("MAILGUN_APIKEY")).thenReturn("abc");

    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);

    Assertions.assertDoesNotThrow(() -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(1)).save(user);
    verify(mailgunEmailService, times(1)).sendPasswordResetConfirmation(user);
    Assertions.assertNull(user.getResetToken());
    Assertions.assertNull(user.getResetPasswordExpiration());
    Assertions.assertNotNull(user.getPassword());
  }

  @Test
  @DisplayName("Confirm reset password no mailgun api token should succeed")
  void confirmResetPasswordForUser_noMailgunApiToken_shouldSucceed() {
    String token = "validToken";
    UserEntity user = new UserEntity();
    user.setResetToken(token);
    user.setResetPasswordExpiration(LocalDateTime.now().plusMinutes(30));

    String newPassword = "NewPassword@123";

    String encodedPassword = "hash@abcxasd123!";
    when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
    user.setPassword(encodedPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
    when(authUtil.validatePassword(newPassword)).thenReturn(Optional.empty());

    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);

    Assertions.assertDoesNotThrow(() -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(1)).save(user);
    verify(mailgunEmailService, times(0)).sendPasswordResetConfirmation(user);
    Assertions.assertNull(user.getResetToken());
    Assertions.assertNull(user.getResetPasswordExpiration());
    Assertions.assertNotNull(user.getPassword());
  }

  @Test
  @DisplayName("Confirm reset password with expired token should fail")
  void confirmResetPasswordForUser_expiredToken_shouldFail() {
    String token = "expiredToken";
    String newPassword = "NewPassword@123";

    UserEntity user = new UserEntity();
    user.setResetToken(token);
    user.setResetPasswordExpiration(LocalDateTime.now().minusHours(3));

    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

    Assertions.assertThrows(
        ResetExpiredException.class, () -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(0)).save(any());
    verify(mailgunEmailService, times(0)).sendPasswordResetConfirmation(any());
  }

  @Test
  @DisplayName("Confirm reset password with invalid token should fail")
  void confirmResetPasswordForUser_invalidToken_shouldFail() {
    String token = "invalidToken";
    String newPassword = "NewPassword@123";
    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        UserNotFoundException.class, () -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(0)).save(any());
    verify(mailgunEmailService, times(0)).sendPasswordResetConfirmation(any());
  }

  @Test
  @DisplayName("Confirm reset password with mismatched passwords should fail")
  void confirmResetPasswordForUser_mismatchedPasswords_shouldFail() {
    String token = "validToken";
    String newPassword = "NewPassword@123";
    String mismatchedPassword = "Mismatch@123";

    UserEntity user = new UserEntity();
    user.setResetToken(token);
    user.setResetPasswordExpiration(LocalDateTime.now().plusMinutes(30));
    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, mismatchedPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

    Assertions.assertThrows(
        BadPasswordException.class, () -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(0)).save(any());
    verify(mailgunEmailService, times(0)).sendPasswordResetConfirmation(any());
  }

  @Test
  @DisplayName("Confirm reset password with invalid password should fail")
  void confirmResetPasswordForUser_invalidPassword_shouldFail() {
    String token = "validToken";
    String newPassword = "weak";

    UserEntity user = new UserEntity();
    user.setResetToken(token);
    user.setResetPasswordExpiration(LocalDateTime.now().plusMinutes(30));

    PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);

    when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
    when(authUtil.validatePassword(newPassword)).thenReturn(Optional.of("Weak password"));

    Assertions.assertThrows(
        BadPasswordException.class, () -> authService.confirmResetPasswordForUser(request));

    verify(userRepository, times(0)).save(any());
    verify(mailgunEmailService, times(0)).sendPasswordResetConfirmation(any());
  }
}
