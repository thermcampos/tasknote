package br.com.tasknoteapp.server.exception;

/** Represents an exception when trying to delete a not archived note. */
public class NoteNotArchivedException extends BaseBadRequestException {
  
  public NoteNotArchivedException() {
    super("note", "Note must be archived");
  }
}
