/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

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
