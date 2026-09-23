package br.com.tasknoteapp.server.exception;

/** This class represents any exception during Note requests. */
public class InvalidNoteException extends BaseBadRequestException {
  
  public InvalidNoteException(String message) {
    super("notes", message);
  }
}
