package br.com.tasknoteapp.server.entity;

/** This class represents a note url in the database. */
public record NoteUrl(Long id, Long noteId, String url) {}
