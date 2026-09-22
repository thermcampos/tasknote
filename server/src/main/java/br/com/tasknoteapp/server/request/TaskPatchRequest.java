package br.com.tasknoteapp.server.request;

import java.util.List;

/** This record represents a task patch payload. */
public record TaskPatchRequest(
    Boolean completed,
    String description,
    List<String> urls,
    String dueDate,
    Boolean highPriority,
    List<String> tags) {}
