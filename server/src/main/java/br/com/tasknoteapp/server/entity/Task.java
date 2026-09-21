package br.com.tasknoteapp.server.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** This class represents a task in the database. */
public record Task(Long id, Long userId, String description, Boolean completed,
    LocalDateTime lastUpdate, LocalDate dueDate, Boolean dueDateNotify,
    Boolean dueDateNotifySent, Boolean highPriority) {}

