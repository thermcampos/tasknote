package br.com.tasknoteapp.server.entity;

/**
 * This class represents a relationship between a Tag and a Note or Task.
 *
 * @param tagId The Tag ID.
 * @param name The Tag name.
 * @param userId The User ID for the tag.
 * @param taskNoteId The foreign key to either Note or Task.
 */
public record TaskNoteTag(Long tagId, String name, Long userId, Long taskNoteId) {}
