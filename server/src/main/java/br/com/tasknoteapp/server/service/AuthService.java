package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.entity.UserPwdLimitEntity;
import br.com.tasknoteapp.server.exception.BadLanguageException;
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
import br.com.tasknoteapp.server.util.SecurityUtil;
import br.com.tasknoteapp.server.util.TokenUtil;
import br.com.tasknoteapp.server.util.UuidUtil;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** This class contains the implementation for the Auth Service class. */
@Service
public class AuthService {

  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  private final JwtService jwtService;

  private final AuthenticationManager authenticationManager;

  private final AuthUtil authUtil;

  private final UserPwdLimitRepository userPwdLimitRepository;

  private final MailgunEmailService mailgunEmailService;

  private final Environment environment;

  /**
   * Constructor for AuthService.
   *
   * @param userRepository UserRepository instance.
   * @param passwordEncoder PasswordEncoder instance.
   * @param jwtService JwtService instance.
   * @param authenticationManager AuthenticationManager instance.
   * @param authUtil AuthUtil instance.
   * @param userPwdLimitRepository UserPwdLimitRepository instance.
   * @param mailgunEmailService MailgunEmailService instance.
   * @param environment Environment instance.
   */
  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager,
      AuthUtil authUtil,
      UserPwdLimitRepository userPwdLimitRepository,
      MailgunEmailService mailgunEmailService,
      Environment environment) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
    this.authUtil = authUtil;
    this.userPwdLimitRepository = userPwdLimitRepository;
    this.mailgunEmailService = mailgunEmailService;
    this.environment = environment;
  }

  /**
   * Create a new user in the app.
   *
   * @param newUser User details with email and password.
   * @return Token
   */
  @Transactional
  public UserResponseWithToken signUpNewUser(LoginRequest newUser) {
    logger.info("Signing up new user: {}", SecurityUtil.redactEmail(newUser.email()));

    if (findByEmail(newUser.email()).isPresent()) {
      throw new EmailAlreadyExistsException();
    }

    Optional<String> passwordValidation = authUtil.validatePassword(newUser.password());
    if (passwordValidation.isPresent()) {
      throw new BadPasswordException(passwordValidation.get());
    }

    if (Objects.isNull(newUser.passwordAgain())
        || !newUser.password().equals(newUser.passwordAgain())) {
      throw new BadPasswordException("The passwords should match");
    }

    String[] validLangs = new String[] {"en", "es", "pt_br", "ru"};
    if (!Arrays.asList(validLangs).contains(newUser.lang())) {
      throw new BadLanguageException();
    }

    UUID emailUuid = new UuidUtil().generateEmailUuid(newUser.email());

    UserEntity user = new UserEntity();
    user.setEmail(newUser.email());
    user.setPassword(passwordEncoder.encode(newUser.password()));
    user.setAdmin(false);
    user.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    user.setLastPasswordChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    user.setEmailUuid(emailUuid);
    user.setLang(newUser.lang());
    userRepository.save(user);

    if (hasValidMailgunApiKey()) {
      mailgunEmailService.sendNewUser(user);
    }

    logger.info("User created! ID {}", user.getId());
    return UserResponseWithToken.fromEntity(
        user, null, getGravatarImageUrl(newUser.email()).orElse(null));
  }

  /**
   * Find a user by email in the database.
   *
   * @param email The user email.
   * @return Optional of a UserEntity instance.
   */
  public Optional<UserEntity> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  /**
   * Load a user from the database given his email.
   *
   * @param email The user email.
   * @return User with found record.
   */
  public User loadUserByUsername(String email) {
    Optional<UserEntity> user = userRepository.findByEmail(email);
    if (user.isEmpty()) {
      throw new UserNotFoundException();
    }

    return new User(user.get().getEmail(), user.get().getPassword(), new ArrayList<>());
  }

  /**
   * SignIn a user given his email and password.
   *
   * @param login User details with email and password.
   * @return Token
   */
  @Transactional
  public UserResponseWithToken signInUser(LoginRequest login) {
    logger.info("Signing in user: {}", SecurityUtil.redactEmail(login.email()));

    Optional<UserEntity> userOptional = findByEmail(login.email());
    if (userOptional.isEmpty()) {
      throw new InvalidCredentialsException();
    }

    checkLoginAttemptLimit(userOptional.get().getId());

    UserEntity user = userOptional.get();

    if (Objects.isNull(user.getEmailConfirmedAt())) {
      logger.warn("User {} tried to login but email is not confirmed", user.getId());
      throw new EmailNotConfirmedException();
    }

    user.setResetToken(null);
    user.setResetPasswordExpiration(null);

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(login.email(), login.password()));

      String token = jwtService.generateToken(user);

      logger.info("User authenticated! Token {}", token.substring(0, 6) + "...");

      final LocalDateTime previousLastLogin = user.getLastLogin();

      user.setLastLogin(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
      userPwdLimitRepository.deleteAllForUser(user.getId());
      userRepository.save(user);

      return new UserResponseWithToken(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getAdmin(),
          user.getCreatedAt(),
          user.getInactivatedAt(),
          previousLastLogin,
          getGravatarImageUrl(login.email()).orElse(null),
          token,
          user.getLang());
    } catch (BadCredentialsException e) {
      logger.error(
          "BadCredentialsException when logging in user {}: {}", user.getId(), e.getMessage());

      // store attempt
      UserPwdLimitEntity pwdLimit = new UserPwdLimitEntity();
      pwdLimit.setWhenHappened(LocalDateTime.now());
      pwdLimit.setUser(user);
      userPwdLimitRepository.save(pwdLimit);

      return null;
    }
  }

  /**
   * Get all registered users. Only allowed for admin users.
   *
   * @return List of UserEntity.
   * @throws UserForbiddenException when the user has no permissions.
   */
  public List<UserResponse> getAllUsers() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    if (currentUserEmail.isEmpty()) {
      logger.error("Unable to get current user from the request");
      throw new UserNotFoundException();
    }

    Optional<UserEntity> currentUserOpt = findByEmail(currentUserEmail.get());
    if (currentUserOpt.isEmpty()) {
      logger.error("Unable to find user by email with value: {}", currentUserEmail.get());
      throw new UserNotFoundException();
    }

    UserEntity currentUser = currentUserOpt.get();
    if (!currentUser.getAdmin()) {
      logger.warn("User {} not allowed to list users.", currentUser.getId());
      throw new UserForbiddenException();
    }

    logger.info("Getting all users to user {}", currentUser.getId());
    List<UserEntity> users = userRepository.findAll();
    List<UserResponse> usersResponse = new ArrayList<>(users.size());
    users.forEach(
        u ->
            usersResponse.add(
                UserResponse.fromEntity(u, getGravatarImageUrl(u.getEmail()).orElse(null))));
    logger.info("{} user(s) found!", usersResponse.size());

    return usersResponse;
  }

  /**
   * Refresh the current user token.
   *
   * @return Token
   */
  public String refreshCurrentUserToken() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    UserEntity currentUser = findByEmail(email).orElseThrow();

    logger.info("Refreshing current session to user {}", currentUser.getId());

    String token = jwtService.generateToken(currentUser);

    logger.info("User refreshed! Token {}...", token.substring(0, 6));
    return token;
  }

  /**
   * Verify the given password against the current user, recording a failed attempt for rate
   * limiting when it does not match.
   *
   * @param user The {@link UserEntity} to check the password against.
   * @param password The plain text password to verify.
   * @throws MaxLoginLimitAttemptException when too many failed attempts happened recently.
   * @throws InvalidCredentialsException when the password does not match.
   */
  public void verifyCurrentPassword(UserEntity user, String password) {
    checkLoginAttemptLimit(user.getId());

    if (Objects.isNull(password) || !passwordEncoder.matches(password, user.getPassword())) {
      logger.warn("Password verification failed for user {}", user.getId());

      UserPwdLimitEntity pwdLimit = new UserPwdLimitEntity();
      pwdLimit.setWhenHappened(LocalDateTime.now());
      pwdLimit.setUser(user);
      userPwdLimitRepository.save(pwdLimit);

      throw new InvalidCredentialsException();
    }
  }

  /**
   * Delete current user account.
   *
   * @return {@link UserResponse} with user data.
   */
  public UserResponse deleteUserAccount() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    UserEntity currentUser = findByEmail(email).orElseThrow();

    logger.info("Deleting account for user {}", currentUser.getId());

    currentUser.setInactivatedAt(LocalDateTime.now());
    userPwdLimitRepository.deleteAllForUser(currentUser.getId());
    userRepository.delete(currentUser);

    return UserResponse.fromEntity(currentUser, getGravatarImageUrl(email).orElse(null));
  }

  /**
   * Patches a user allowing him to update his information.
   *
   * @param patchRequest An instance of {@link UserPatchRequest} with the user data.
   * @return UserResponse containing the updated info.
   */
  @Transactional
  public UserResponse patchUserInfo(UserPatchRequest patchRequest) {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    String email = currentUserEmail.orElseThrow();
    UserEntity currentUser = findByEmail(email).orElseThrow();
    boolean shouldUpdate = false;
    boolean emailChanged = false;

    boolean changingEmail =
        !Objects.isNull(patchRequest.email()) && !patchRequest.email().isBlank();
    boolean changingPassword =
        !Objects.isNull(patchRequest.password()) && !patchRequest.password().isBlank();

    if (changingEmail || changingPassword) {
      if (Objects.isNull(patchRequest.currentPassword())
          || patchRequest.currentPassword().isBlank()) {
        throw new BadPasswordException("Current password is required to change email or password");
      }
      if (!passwordEncoder.matches(patchRequest.currentPassword(), currentUser.getPassword())) {
        throw new InvalidCredentialsException();
      }
    }

    if (!Objects.isNull(patchRequest.name()) && !patchRequest.name().isBlank()) {
      currentUser.setName(patchRequest.name().trim());
      shouldUpdate = true;
    }
    if (changingEmail) {
      currentUser.setEmail(patchRequest.email().trim());
      shouldUpdate = true;
      emailChanged = true;
    }
    if (!Objects.isNull(patchRequest.lang()) && !patchRequest.lang().isBlank()) {
      currentUser.setLang(patchRequest.lang());
      shouldUpdate = true;
    }
    if (!Objects.isNull(patchRequest.theme()) && !patchRequest.theme().isBlank()) {
      String[] validThemes = new String[] {"light", "dark"};
      if (!Arrays.asList(validThemes).contains(patchRequest.theme())) {
        throw new BadThemeException();
      }
      currentUser.setTheme(patchRequest.theme());
      shouldUpdate = true;
    }

    boolean updatePassword =
        changingPassword
            && !Objects.isNull(patchRequest.passwordAgain())
            && !patchRequest.passwordAgain().isBlank();

    if (updatePassword) {
      Optional<String> passwordValidation = authUtil.validatePassword(patchRequest.password());
      if (passwordValidation.isPresent()) {
        throw new BadPasswordException(passwordValidation.get());
      }

      if (!patchRequest.password().equals(patchRequest.passwordAgain())) {
        throw new BadPasswordException("The passwords should match");
      }

      currentUser.setPassword(passwordEncoder.encode(patchRequest.password()));
      currentUser.setLastPasswordChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
      shouldUpdate = true;
    }

    if (shouldUpdate) {
      userRepository.save(currentUser);
    }

    if (emailChanged && hasValidMailgunApiKey()) {
      // send email to older and new account
      logger.info(
          "Email changed from {} to {}", email, SecurityUtil.redactEmail(patchRequest.email()));
      mailgunEmailService.sendEmailChangedNotification(currentUser, email);
    }

    return UserResponse.fromEntity(currentUser, getGravatarImageUrl(email).orElse(null));
  }

  /**
   * Get the current logged user (based in the JWT Authentication).
   *
   * @return An instance of {@link UserEntity} with the current user.
   * @throws UserNotFoundException when the user was not found
   */
  public Optional<UserEntity> getCurrentUser() {
    Optional<String> currentUserEmail = authUtil.getCurrentUserEmail();
    if (currentUserEmail.isEmpty()) {
      throw new UserNotFoundException();
    }
    String email = currentUserEmail.get();
    return findByEmail(email);
  }

  /**
   * Get the current logged user as response object.
   *
   * @return An instance of {@link UserResponse} with the current user.
   * @throws UserNotFoundException when the user was not found
   */
  public UserResponse getCurrentUserResponse() {
    UserEntity user = getCurrentUser().orElseThrow(UserNotFoundException::new);
    return UserResponse.fromEntity(user, getGravatarImageUrl(user.getEmail()).orElse(null));
  }

  /**
   * Confirm a user account.
   *
   * @param identification The UUID generated when registering.
   * @throws BadUuidException if bad identification
   */
  @Transactional
  public void confirmUserAccount(String identification) {
    logger.info("Confirming user email account");
    UUID uuid;

    try {
      uuid = UUID.fromString(identification);
    } catch (IllegalArgumentException ex) {
      throw new BadUuidException();
    }

    Optional<UserEntity> userOptional = userRepository.findByEmailUuid(uuid);
    if (userOptional.isEmpty()) {
      throw new UserNotFoundException();
    }

    UserEntity user = userOptional.get();
    user.setEmailConfirmedAt(LocalDateTime.now());

    userRepository.save(user);
    logger.info("User email address confirmed: {}", identification);
  }

  /**
   * Re-send the confirmation email to the user.
   *
   * @param email The email to re-send.
   */
  public void resendEmailConfirmation(String email) {
    logger.info("Re-sending the confirmation email");

    Optional<UserEntity> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UserNotFoundException();
    }

    UserEntity user = userOptional.get();

    if (hasValidMailgunApiKey()) {
      mailgunEmailService.sendNewUser(user);
    }

    logger.info("Confirmation email re-sent!");
  }

  /**
   * Request the password reset for the user.
   *
   * @param email The user email.
   */
  @Transactional
  public void resetPasswordForUser(String email) {
    logger.info("Requesting password reset for email {}", email);

    Optional<UserEntity> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      logger.info("No user found with email {}", email);
      return;
    }

    String resetToken = new TokenUtil().generateToken();

    UserEntity user = userOptional.get();
    user.setResetToken(resetToken);
    user.setResetPasswordExpiration(
        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusHours(2L));

    userRepository.save(user);
    if (hasValidMailgunApiKey()) {
      mailgunEmailService.sendResetPassword(user);
    }

    logger.info("Password reset request succeeded");
  }

  /**
   * Confirm the password reset and recreate the password for the user.
   *
   * @param request The token and new passwords.
   */
  @Transactional
  public void confirmResetPasswordForUser(PasswordResetRequest request) {
    logger.info("Saving new password for token {}", request.token());

    Optional<UserEntity> userOptional = userRepository.findByResetToken(request.token());
    if (userOptional.isEmpty()) {
      throw new UserNotFoundException();
    }

    LocalDateTime requestTime = userOptional.get().getResetPasswordExpiration();
    boolean isMoreThan2Hours =
        Duration.between(LocalDateTime.now(), requestTime).abs().toHours() > 2;
    if (isMoreThan2Hours) {
      throw new ResetExpiredException();
    }

    Optional<String> passwordValidation = authUtil.validatePassword(request.password());
    if (passwordValidation.isPresent()) {
      throw new BadPasswordException(passwordValidation.get());
    }

    if (!request.password().equals(request.passwordAgain())) {
      throw new BadPasswordException("The passwords should match");
    }

    UserEntity user = userOptional.get();
    user.setResetToken(null);
    user.setResetPasswordExpiration(null);
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setLastPasswordChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

    userRepository.save(user);
    if (hasValidMailgunApiKey()) {
      mailgunEmailService.sendPasswordResetConfirmation(user);
    }

    logger.info("New password set for token {}", request.token());
  }

  private Optional<String> getGravatarImageUrl(String email) {
    email = email.toLowerCase().trim();
    logger.info("Current user email: {}", SecurityUtil.redactEmail(email));

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      logger.debug("Email hashed: {}", hexString);
      return Optional.of(hexString.toString());
    } catch (NoSuchAlgorithmException | NullPointerException e) {
      logger.error("NoSuchAlgorithmException or NullPointerException: {}", e.getMessage());
    }
    return Optional.empty();
  }

  private void checkLoginAttemptLimit(Long userId) {
    // Fetch only the 3 most recent failed attempts to avoid loading unbounded rows for
    // targeted/brute-forced accounts.
    List<UserPwdLimitEntity> userPwdList =
        userPwdLimitRepository.findTop3ByUser_idOrderByWhenHappenedDesc(userId);

    logger.warn("login count attempt for user {}: {}", userId, userPwdList.size());

    // if it's more than 3 times in the last 10 minutes, raise timer of 3 hours.
    if (userPwdList.size() >= 3) {
      UserPwdLimitEntity oldest = userPwdList.getLast();
      logger.warn("Oldest failed attempt: {}", oldest.getWhenHappened());
      Duration duration = Duration.between(oldest.getWhenHappened(), LocalDateTime.now());
      if (duration.toMinutes() <= 3L) {
        logger.warn("Account locked, minutes remaining: {}", 3L - duration.toMinutes());
        throw new MaxLoginLimitAttemptException();
      }
    }
  }

  private boolean hasValidMailgunApiKey() {
    String apiKey = environment.getProperty("MAILGUN_APIKEY");
    return Optional.ofNullable(apiKey).isPresent()
        && !"invalid-api-key-only-placeholder".equals(apiKey);
  }

}
