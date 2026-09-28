package br.com.tasknoteapp.server.filter;

import br.com.tasknoteapp.server.service.JwtService;
import br.com.tasknoteapp.server.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** This class represents an authentication filter do authenticate requests. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String REFRESH_PATH = "/rest/user-sessions/refresh";

  private final UserService userService;

  private final JwtService jwtService;

  JwtAuthenticationFilter(UserService userService,  JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String authorizationHeader = request.getHeader("Authorization");

    // Skip authentication for paths that don't require it
    String requestPath = request.getServletPath();
    if (requestPath.startsWith("/auth/") || !requestPath.startsWith("/rest/")) {
      filterChain.doFilter(request, response);
      return;
    }

    // For protected paths, require valid authentication
    if (Objects.isNull(authorizationHeader) || authorizationHeader.isBlank()) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("Unauthorized: Missing authentication token");
      return;
    }

    String jwtToken = authorizationHeader;
    
    if (authorizationHeader.startsWith("Bearer ")) {
      jwtToken = authorizationHeader.substring(7);
    }
    
    String email = jwtService.getEmailFromToken(jwtToken);

    if (Objects.isNull(email)) {
      throw new ServletException("Invalid token: email not found");
    }

    UserDetails user = userService.userDetailsService().loadUserByUsername(email);

    boolean valid = jwtService.validateTokenAndUser(jwtToken, user);

    // Allow a recently expired token to be exchanged only on the refresh endpoint, so a
    // waking or previously frozen tab can resume its session without a full re-login.
    if (!valid && REFRESH_PATH.equals(requestPath)) {
      valid = jwtService.validateTokenForRefresh(jwtToken, user);
    }

    if (!valid) {
      throw new ServletException("Invalid token for user");
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(
            user.getUsername(), user.getPassword(), user.getAuthorities());

    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    context.setAuthentication(authToken);
    SecurityContextHolder.setContext(context);

    filterChain.doFilter(request, response);
  }
}
