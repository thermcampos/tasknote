package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.request.UserPatchRequest;
import br.com.tasknoteapp.server.response.UserResponse;
import br.com.tasknoteapp.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This class contains resources for handling users admin requests. */
@RestController
@RequestMapping("/rest/users")
public class UserController {

  private final AuthService authService;

  public UserController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Get the current logged user.
   *
   * @return UserEntity with the current user information.
   */
  @GetMapping("/me")
  public UserResponse getCurrentUser() {
    return authService.getCurrentUserResponse();
  }

  @PatchMapping
  public ResponseEntity<UserResponse> patchUserInfo(@RequestBody UserPatchRequest taskRequest) {
    UserResponse patched = authService.patchUserInfo(taskRequest);
    return ResponseEntity.ok().body(patched);
  }
}
