package br.com.tasknoteapp.server.service.impl;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** This class contains the implementation for the Jwt Service class. */
@Service
class JwtServiceImpl implements JwtService {

  private static final long SECOND = 1000;
  private static final long MINUTE = SECOND * 60;
  private static final long EXPIRATION_TIME = MINUTE * 30;

  // How long after expiration a token may still be exchanged for a new one on the refresh
  // endpoint, so sleep/wake or frozen tabs do not force a full re-login.
  private static final Duration REFRESH_GRACE_PERIOD = Duration.ofHours(12);
  private final SecretKey key;

  public JwtServiceImpl(@Value("${br.com.tasknote.server.jwt-secret}") String secretKey) {
    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  @Override
  public String getEmailFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  @Override
  public LocalDateTime extractExpiration(String token) {
    Date date = extractClaim(token, Claims::getExpiration);
    if (!Objects.isNull(date)) {
      return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
    return null;
  }

  private LocalDateTime extractIssuedAt(String token) {
    Date date = extractClaim(token, Claims::getIssuedAt);
    if (!Objects.isNull(date)) {
      return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
    return null;
  }

  @Override
  public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("name", user.getName());

    List<String> roles = new ArrayList<>();
    if (Boolean.TRUE.equals(user.getAdmin())) {
      roles.add("admin");
    }

    claims.put("roles", roles);
    return createToken(claims, user.getEmail());
  }

  @Override
  public String createToken(Map<String, Object> claims, String email) {
    return Jwts.builder()
        .issuer("Java-API")
        .subject(email)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .claims(claims)
        .signWith(key)
        .compact();
  }

  @Override
  public boolean isTokenExpired(String token) {
    LocalDateTime expiration = extractExpiration(token);
    if (expiration != null) {
      return expiration.isBefore(LocalDateTime.now());
    }
    return true;
  }

  @Override
  public boolean validateTokenAndUser(String token, UserDetails user) {
    final String email = user.getUsername();
    boolean basicValid = !isTokenExpired(token) && email.equals(getEmailFromToken(token));
    return basicValid && isIssuedAfterLastPasswordChange(token, user);
  }

  @Override
  public boolean validateTokenForRefresh(String token, UserDetails user) {
    final String email = user.getUsername();
    if (!email.equals(getEmailFromToken(token))) {
      return false;
    }

    LocalDateTime expiration = extractExpiration(token);
    boolean withinGracePeriod =
        expiration != null
            && expiration.isAfter(LocalDateTime.now().minus(REFRESH_GRACE_PERIOD));

    return withinGracePeriod && isIssuedAfterLastPasswordChange(token, user);
  }

  private boolean isIssuedAfterLastPasswordChange(String token, UserDetails user) {
    if (user instanceof User userEntity) {
      LocalDateTime iat = extractIssuedAt(token);
      if (iat != null && userEntity.getLastPasswordChange() != null) {
        // Token must be issued after or at the same time as last password change
        // We use isBefore to invalidate tokens issued BEFORE the change
        return !iat.isBefore(userEntity.getLastPasswordChange());
      }
    }
    return true;
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Optional<Claims> claims = extractAllClaims(token);
    return claims.map(claimsResolver).orElse(null);
  }

  private Optional<Claims> extractAllClaims(String token) {
    try {
      return Optional.of(
          Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    } catch (ExpiredJwtException e) {
      // Signature is still valid: expose the claims so callers can decide about grace periods.
      return Optional.of(e.getClaims());
    } catch (JwtException e) {
      return Optional.empty();
    }
  }
}
