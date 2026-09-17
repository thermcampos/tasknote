package br.com.tasknoteapp.server.service.impl;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.repository.UserRepository;
import br.com.tasknoteapp.server.service.UserService;
import br.com.tasknoteapp.server.util.SecurityUtil;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/** This class contains the implementation for the User Service class. */
@Service
class UserServiceImpl implements UserService {

  private final UserDetailsService cachedUserDetailsService;

  public UserServiceImpl(UserRepository userRepository) {
    this.cachedUserDetailsService =
        email -> {
          Optional<User> user = userRepository.findByEmail(email);
          if (user.isEmpty()) {
            throw new RuntimeException("User not found: " + SecurityUtil.redactEmail(email));
          }
          return user.get();
        };
  }

  @Override
  public UserDetailsService userDetailsService() {
    return this.cachedUserDetailsService;
  }
}
