package br.com.tasknoteapp.server.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** This class represents a User in the database. */
public class User implements UserDetails {

  private Long id;
  private String email;
  private String password;
  private Boolean admin;
  private LocalDateTime createdAt;
  private LocalDateTime inactivatedAt;
  private String name;
  private LocalDateTime emailConfirmedAt;
  private UUID emailUuid;
  private LocalDateTime resetPasswordExpiration;
  private String resetToken;
  private String lang;
  private String theme;
  private LocalDateTime lastPasswordChange;
  private LocalDateTime lastLogin;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getUsername() {
    // email in our case
    return email;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Boolean getAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getInactivatedAt() {
    return inactivatedAt;
  }

  public void setInactivatedAt(LocalDateTime inactivatedAt) {
    this.inactivatedAt = inactivatedAt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getEmailConfirmedAt() {
    return emailConfirmedAt;
  }

  public void setEmailConfirmedAt(LocalDateTime emailConfirmedAt) {
    this.emailConfirmedAt = emailConfirmedAt;
  }

  public UUID getEmailUuid() {
    return emailUuid;
  }

  public void setEmailUuid(UUID emailUuid) {
    this.emailUuid = emailUuid;
  }

  public LocalDateTime getResetPasswordExpiration() {
    return resetPasswordExpiration;
  }

  public void setResetPasswordExpiration(LocalDateTime resetPasswordExpiration) {
    this.resetPasswordExpiration = resetPasswordExpiration;
  }

  public String getResetToken() {
    return resetToken;
  }

  public void setResetToken(String resetToken) {
    this.resetToken = resetToken;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public LocalDateTime getLastPasswordChange() {
    return lastPasswordChange;
  }

  public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
    this.lastPasswordChange = lastPasswordChange;
  }

  public LocalDateTime getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(LocalDateTime lastLogin) {
    this.lastLogin = lastLogin;
  }
}
