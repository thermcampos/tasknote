package br.com.tasknoteapp.server.entity;

import java.time.LocalDateTime;

/** This class represents a User Password Limit in the database. */
public record UserPwdLimit(Long id, LocalDateTime whenHappened, Long userId) {}

