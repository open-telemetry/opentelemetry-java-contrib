package io.opentelemetry.opamp.client.internal.response;

public class OpampServerResponseError extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs an OpAMP error related exception.
   *
   * @param message The OpAMP error message.
   */
  public OpampServerResponseError(String message) {
    super(message);
  }
}
