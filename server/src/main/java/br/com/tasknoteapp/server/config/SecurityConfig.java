package br.com.tasknoteapp.server.config;

import br.com.tasknoteapp.server.filter.JwtAuthenticationFilter;
import br.com.tasknoteapp.server.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** This class contains security configurations. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserService userService;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(UserService userService, JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.userService = userService;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  /**
   * Filters a request to add security checks and configurations.
   *
   * @param http instance of HttpSecurity containing the request.
   * @return SecurityFilterChain with allowed endpoints and all configuration.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            request ->
                request
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/auth/**")
                    .permitAll()
                    .requestMatchers("/public/**")
                    .permitAll()
                    .requestMatchers("/rest/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.getWriter().write("Unauthorized: " + authException.getMessage());
                    }))
        .authenticationProvider(authenticationProvider());

    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Provides an AuthenticationProvider with user and password.
   *
   * @return AuthenticationProvider instance
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider =
        new DaoAuthenticationProvider(userService.userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * Provides an AuthenticationManager to manage authentication.
   *
   * @param config AuthenticationConfiguration instance
   * @return AuthenticationManager instance
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();
  }
}
