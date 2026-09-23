package br.com.tasknoteapp.server.request;

import java.util.List;

/** This record represents a task request to be created. */
public record TaskRequest(
    String description,
    List<String> urls,
    String dueDate,
    Boolean highPriority,
    List<String> tags) {}
