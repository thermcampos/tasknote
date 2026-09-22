package br.com.tasknoteapp.server.service;

import br.com.tasknoteapp.server.entity.User;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

/** This interface contains methods for handling user JWT tokens. */
public interface JwtService {

  String getEmailFromToken(String token);

  LocalDateTime extractExpiration(String token);

  String generateToken(User user);

  String createToken(Map<String, Object> claims, String email);

  boolean isTokenExpired(String token);

  boolean validateTokenAndUser(String token, UserDetails user);
}
