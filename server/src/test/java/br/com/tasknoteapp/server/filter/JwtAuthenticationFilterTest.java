package br.com.tasknoteapp.server.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.service.JwtService;
import br.com.tasknoteapp.server.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  private static final String REFRESH_PATH = "/rest/user-sessions/refresh";
  private static final String TOKEN = "token";
  private static final String EMAIL = "user@example.com";

  private JwtAuthenticationFilter filter;

  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;
  @Mock private UserDetailsService userDetailsService;
  @Mock private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(userService, jwtService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void stubTokenRequest(String path) {
    when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    when(request.getServletPath()).thenReturn(path);
    when(jwtService.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
    when(userService.userDetailsService()).thenReturn(userDetailsService);
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
    lenient().when(userDetails.getUsername()).thenReturn(EMAIL);
    lenient().when(userDetails.getPassword()).thenReturn("password");
    lenient().when(userDetails.getAuthorities()).thenReturn(List.of());
  }

  @Test
  void doFilterInternal_validToken_authenticates() throws ServletException, IOException {
    stubTokenRequest("/rest/home");
    when(jwtService.validateTokenAndUser(TOKEN, userDetails)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).validateTokenForRefresh(any(), any());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_expiredTokenOnRefreshPathWithinGrace_authenticates()
      throws ServletException, IOException {
    stubTokenRequest(REFRESH_PATH);
    when(jwtService.validateTokenAndUser(TOKEN, userDetails)).thenReturn(false);
    when(jwtService.validateTokenForRefresh(TOKEN, userDetails)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_expiredTokenOnOtherPath_rejected() {
    stubTokenRequest("/rest/home");
    when(jwtService.validateTokenAndUser(TOKEN, userDetails)).thenReturn(false);

    assertThrows(
        ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));
    verify(jwtService, never()).validateTokenForRefresh(any(), any());
  }

  @Test
  void doFilterInternal_expiredTokenBeyondGraceOnRefreshPath_rejected() {
    stubTokenRequest(REFRESH_PATH);
    when(jwtService.validateTokenAndUser(TOKEN, userDetails)).thenReturn(false);
    when(jwtService.validateTokenForRefresh(TOKEN, userDetails)).thenReturn(false);

    assertThrows(
        ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));
  }
}
