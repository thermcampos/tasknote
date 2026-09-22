package br.com.tasknoteapp.server.exception;

/** This class represents any exception during Tasks requests. */
public class InvalidTaskException extends BaseBadRequestException {
  
  public InvalidTaskException(String message) {
    super("tasks", message);
  }
}
