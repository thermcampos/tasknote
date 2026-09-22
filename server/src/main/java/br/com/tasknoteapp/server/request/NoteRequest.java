package br.com.tasknoteapp.server.request;


import java.util.List;

/** This record represents a note request to be created. */
public record NoteRequest(
    String title,
    String description,
    String url,
    List<String> tags) {}
