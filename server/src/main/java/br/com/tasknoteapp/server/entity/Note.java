package br.com.tasknoteapp.server.entity;

import java.time.LocalDateTime;

/** This class represents a note in the database. */
public record Note(Long id, Long userId, String description, String title,
    LocalDateTime lastUpdate, Boolean shared, String shareToken,
    Boolean archived) {}

