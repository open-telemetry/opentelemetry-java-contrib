/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

/**
 * Exception that carries an HTTP error code and message.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class HttpErrorException extends Exception {
  private final int errorCode;

  private static final long serialVersionUID = 1L;

  public int getErrorCode() {
    return errorCode;
  }

  /**
   * Constructs an HTTP error related exception.
   *
   * @param errorCode The HTTP error code.
   * @param message The HTTP error message associated with the code.
   */
  public HttpErrorException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
