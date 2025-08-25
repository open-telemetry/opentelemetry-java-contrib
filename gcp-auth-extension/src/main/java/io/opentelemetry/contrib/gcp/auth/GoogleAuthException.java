/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

/**
 * An unchecked exception indicating a failure during Google authentication. This exception is
 * thrown when there are issues with retrieving or refreshing Google Application Default Credentials
 * (ADC).
 */
public final class GoogleAuthException extends RuntimeException {

  private static final long serialVersionUID = 149908685226796448L;

  /**
   * Constructs a new {@code GoogleAuthException} with the specified reason and cause.
   *
   * @param reason the reason for the authentication failure.
   * @param cause the underlying cause of the exception (e.g., an IOException).
   */
  GoogleAuthException(Reason reason, Throwable cause) {
    super(reason.message, cause);
  }

  /** Enumerates the possible reasons for a Google authentication failure. */
  enum Reason {
    /** Indicates a failure to retrieve Google Application Default Credentials. */
    FAILED_ADC_RETRIEVAL("Unable to retrieve Google Application Default Credentials."),
    /** Indicates a failure to retrieve Google Application Default Credentials. */
    FAILED_ADC_REFRESH("Unable to refresh Google Application Default Credentials.");

    private final String message;

    /**
     * Constructs a new {@code Reason} with the specified message.
     *
     * @param message the message describing the reason.
     */
    Reason(String message) {
      this.message = message;
    }

    /**
     * Returns the message associated with this reason.
     *
     * @return the message describing the reason.
     */
    public String getMessage() {
      return message;
    }
  }
}
