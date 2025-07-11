/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.response;

import opamp.proto.ServerErrorResponse;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class OpampServerResponseException extends Exception {
  private static final long serialVersionUID = 1L;

  public final ServerErrorResponse errorResponse;

  /**
   * Constructs an OpAMP error related exception.
   *
   * @param errorResponse The OpAMP error.
   * @param message The OpAMP error message.
   */
  public OpampServerResponseException(ServerErrorResponse errorResponse, String message) {
    super(message);
    this.errorResponse = errorResponse;
  }
}
