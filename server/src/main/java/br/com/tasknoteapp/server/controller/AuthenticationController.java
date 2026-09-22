package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.exception.EmailAlreadyExistsException;
import br.com.tasknoteapp.server.exception.InvalidCredentialsException;
import br.com.tasknoteapp.server.exception.UserNotFoundException;
import br.com.tasknoteapp.server.request.EmailConfirmationRequest;
import br.com.tasknoteapp.server.request.LoginRequest;
import br.com.tasknoteapp.server.request.PasswordResetRequest;
import br.com.tasknoteapp.server.request.ResendConfirmationRequest;
import br.com.tasknoteapp.server.response.UserResponseWithToken;
import br.com.tasknoteapp.server.service.AuthService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This class contains the resources for handling authentication. */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

  private final AuthService authService;

  public AuthenticationController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Signup a new user.
   *
   * @param loginRequest User data with email and password.
   * @return JwtAuthenticationResponse containing user token
   * @throws EmailAlreadyExistsException when the provide email is already in use.
   */
  @PutMapping(path = "/sign-up", consumes = "application/json", produces = "application/json")
  public ResponseEntity<Void> signUp(@RequestBody LoginRequest loginRequest) {
    authService.signUpNewUser(loginRequest);
    return ResponseEntity.noContent().build();
  }

  /**
   * Authenticate a user given his email and password.
   *
   * @param loginRequest User data containing email and password.
   * @return JwtAuthenticationResponse with token created.
   * @throws UserNotFoundException if user not or InvalidCredentialsException if credentials are
   *     invalid.
   */
  @PostMapping(path = "/sign-in", consumes = "application/json", produces = "application/json")
  public ResponseEntity<UserResponseWithToken> signIn(@RequestBody LoginRequest loginRequest) {
    UserResponseWithToken response = authService.signInUser(loginRequest);
    if (Objects.isNull(response)) {
      throw new InvalidCredentialsException();
    }
    return ResponseEntity.ok().body(response);
  }

  /**
   * Send a confirmation email to the user.
   *
   * @param confirmation The request containing the user uuid.
   * @return No content 204 http code.
   */
  @PostMapping(path = "/email-confirmation", consumes = "application/json")
  public ResponseEntity<Void> confirmEmailAddress(
        @RequestBody EmailConfirmationRequest confirmation) {
    authService.confirmUserAccount(confirmation.identification());
    return ResponseEntity.noContent().build();
  }

  /**
   * Re-Send a confirmation email to the user.
   *
   * @param request The request containing user's email.
   * @return No content 204 http code.
   */
  @PostMapping(path = "/resend-email-confirmation", consumes = "application/json")
  public ResponseEntity<Void> resendEmailConfirmation(
        @RequestBody ResendConfirmationRequest request) {
    authService.resendEmailConfirmation(request.email());
    return ResponseEntity.noContent().build();
  }

  /**
   * Request a user's password reset.
   *
   * @param request The request containing user's email.
   * @return No content 204 http code.
   */
  @PostMapping(path = "/password-reset", consumes = "application/json")
  public ResponseEntity<Void> passwordReset(@RequestBody ResendConfirmationRequest request) {
    authService.resetPasswordForUser(request.email());
    return ResponseEntity.noContent().build();
  }

  /**
   * Confirm the user password change request.
   *
   * @param request The request containing the token and the new password.
   * @return No content 204 http code.
   */
  @PostMapping(path = "/complete-password-reset", consumes = "application/json")
  public ResponseEntity<Void> completePasswordReset(
        @RequestBody PasswordResetRequest request) {
    authService.confirmResetPasswordForUser(request);
    return ResponseEntity.noContent().build();
  }
}
