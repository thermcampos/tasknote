package br.com.tasknoteapp.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** This class represents a User in the database. */
@Entity
@Table(name = "users")
public class UserEntity implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Boolean admin;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "inactivated_at")
  private LocalDateTime inactivatedAt;

  @Column(name = "name", length = 20)
  private String name;

  @Column(name = "email_confirmed_at")
  private LocalDateTime emailConfirmedAt;

  @Column(name = "email_uuid", columnDefinition = "uuid", unique = true)
  private UUID emailUuid;

  @Column(name = "reset_password_expiration")
  private LocalDateTime resetPasswordExpiration;

  @Column(name = "reset_token", length = 35)
  private String resetToken;

  @Column(name = "lang", length = 6)
  private String lang;

  @Column(name = "theme", length = 10)
  private String theme;

  @Column(name = "last_password_change", nullable = false)
  private LocalDateTime lastPasswordChange;

  @Column(name = "last_login")
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
