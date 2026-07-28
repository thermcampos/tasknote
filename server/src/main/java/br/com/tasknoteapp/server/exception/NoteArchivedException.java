package br.com.tasknoteapp.server.exception;

/** This class represents a conflict when an note is archived and cannot be modified. */
public class NoteArchivedException extends BaseBadRequestException {

  public NoteArchivedException() {
    super("note", "Note is archived");
  }
}
