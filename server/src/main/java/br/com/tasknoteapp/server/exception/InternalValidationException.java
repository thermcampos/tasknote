package br.com.tasknoteapp.server.exception;

/** This class represents an internal validation exception. */
public class InternalValidationException extends RuntimeException {
  
  private final String errorKey;

  /**
   * Creates an instance of InternalValidationException with error key and message.
   *
   * @param errorKey The error key to send to client.
   * @param errorMessage The error message.
   */
  public InternalValidationException(String errorKey, String errorMessage) {
    super(errorMessage);
    this.errorKey = errorKey;
  }

  public String getErrorKey() {
    return errorKey;
  }
}
