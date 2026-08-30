package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.exception.UserNotFoundException;
import br.com.tasknoteapp.server.request.DeleteAccountRequest;
import br.com.tasknoteapp.server.response.JwtAuthenticationResponse;
import br.com.tasknoteapp.server.response.UserResponse;
import br.com.tasknoteapp.server.service.AuthService;
import br.com.tasknoteapp.server.service.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This class contains resources for handling user sessions. */
@RestController
@RequestMapping("/rest/user-sessions")
public class UserSessionController {

  private final UserSessionService userSessionService;

  private final AuthService authService;

  public UserSessionController(UserSessionService userSessionService, AuthService authService) {
    this.userSessionService = userSessionService;
    this.authService = authService;
  }

  /**
   * Refresh an existing user session, generating a new token.
   *
   * @return JwtAuthenticationResponse with token created.
   * @throws UserNotFoundException if user not found
   */
  @GetMapping("/refresh")
  public JwtAuthenticationResponse refresh() {
    return userSessionService.refreshUserSession();
  }

  /**
   * Delete all the user data and information from the server.
   *
   * @param request {@link DeleteAccountRequest} with the current user password.
   * @return {@link UserResponse} with the user information.
   */
  @PostMapping("/delete-account")
  public ResponseEntity<UserResponse> deleteAccount(
      @RequestBody @Valid DeleteAccountRequest request) {
    UserEntity user = authService.getCurrentUser().orElseThrow(UserNotFoundException::new);
    authService.verifyCurrentPassword(user, request.password());
    UserResponse deleted = userSessionService.deleteCurrentUserAccount();
    return ResponseEntity.ok(deleted);
  }
}
