package br.com.tasknoteapp.server.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

  private JwtServiceImpl jwtService;

  @Mock private UserDetails userDetails;

  private final String testSecretKey = "YourTestSecretKeyHereShouldBeAtLeast32BytesLong";
  private final String testEmail = "test@example.com";
  private final String testName = "Test User";
  private final Long testUserId = 1L;

  private SecretKey getKey() {
    return (SecretKey) ReflectionTestUtils.getField(jwtService, "key");
  }

  @BeforeEach
  void setUp() {
    jwtService = new JwtServiceImpl(testSecretKey);
  }

  @Test
  void generateToken_shouldCreateValidToken() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(true);
    user.setName(testName);
    String token = jwtService.generateToken(user);

    assertNotNull(token);

    String emailFromToken = jwtService.getEmailFromToken(token);
    assertNotNull(emailFromToken);
    assertEquals(emailFromToken, testEmail);

    Claims claims = extractClaims(token);
    String userIdClaim = String.valueOf(claims.get("userId"));

    assertEquals(userIdClaim.substring(0, 1), testUserId.toString());
    assertEquals(claims.get("email"), testEmail);
    assertEquals(claims.get("name"), testName);

    List<?> roles = (List<?>) claims.get("roles");
    assertEquals(1, roles.size());
    assertEquals("admin", roles.get(0));
  }

  @Test
  void getEmailFromToken_shouldReturnCorrectEmail() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);

    String token = jwtService.generateToken(user);
    String email = jwtService.getEmailFromToken(token);

    assertEquals(email, testEmail);
  }

  @Test
  void extractExpiration_shouldReturnCorrectExpirationDate() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);

    String token = jwtService.generateToken(user);
    LocalDateTime expiration = jwtService.extractExpiration(token);

    assertNotNull(expiration);
    LocalDateTime expectedExpiration = LocalDateTime.now().plusMinutes(30).withNano(0);
    assertFalse(ChronoUnit.SECONDS.between(expiration.withNano(0), expectedExpiration) > 5);
  }

  @Test
  void isTokenExpired_shouldReturnFalseForValidToken() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);

    String token = jwtService.generateToken(user);
    boolean expired = jwtService.isTokenExpired(token);

    assertFalse(expired);
  }

  @Test
  void isTokenExpired_shouldReturnTrueForExpiredToken() {
    Map<String, Object> claims = Map.of("test", "value");
    String expiredToken =
        Jwts.builder()
            .issuer("Java-API")
            .subject(testEmail)
            .issuedAt(new Date(System.currentTimeMillis() - 2000))
            .expiration(new Date(System.currentTimeMillis() - 1000)) // Expired 1 second ago
            .claims(claims)
            .signWith(getKey())
            .compact();

    assertTrue(jwtService.isTokenExpired(expiredToken));
  }

  @Test
  void validateTokenAndUser_shouldReturnTrueForValidTokenAndMatchingUser() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);
    String token = jwtService.generateToken(user);

    when(userDetails.getUsername()).thenReturn(testEmail);
    boolean valid = jwtService.validateTokenAndUser(token, userDetails);

    assertTrue(valid);
  }

  @Test
  void validateTokenAndUser_shouldReturnFalseForValidTokenButDifferentUser() {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);
    String token = jwtService.generateToken(user);

    UserDetails differentUser = mock(UserDetails.class);
    when(differentUser.getUsername()).thenReturn("different@example.com");

    boolean valid = jwtService.validateTokenAndUser(token, differentUser);

    assertFalse(valid);
  }

  @Test
  void validateTokenAndUser_shouldReturnFalseIfTokenIssuedBeforeLastPasswordChange()
      throws InterruptedException {
    UserEntity user = new UserEntity();
    user.setId(testUserId);
    user.setEmail(testEmail);
    user.setAdmin(false);
    user.setName(testName);
    user.setLastPasswordChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

    // Token issued NOW
    String token = jwtService.generateToken(user);

    // Update lastPasswordChange to FUTURE (simulating a password change after token issuance)
    // We wait 1 second to ensure the new timestamp is strictly after token iat (which has second
    // precision)
    Thread.sleep(1100);
    user.setLastPasswordChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

    boolean valid = jwtService.validateTokenAndUser(token, user);

    assertFalse(valid, "Token issued before password change should be invalid");
  }

  private Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
  }
}
