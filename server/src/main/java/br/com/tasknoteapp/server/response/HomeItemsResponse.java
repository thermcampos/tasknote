package br.com.tasknoteapp.server.response;

import java.util.List;

/** This record represents the Home aggregated payload with tasks and notes. */
public record HomeItemsResponse(List<TaskResponse> tasks, List<NoteResponse> notes) {}
