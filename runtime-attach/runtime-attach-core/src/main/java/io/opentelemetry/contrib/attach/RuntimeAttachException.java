/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

/**
 * Exception that gets thrown if a problem occurs during the attachment of the OpenTelemetry agent.
 */
public final class RuntimeAttachException extends RuntimeException {

  private static final long serialVersionUID = 1982913847038355735L;

  private RuntimeAttachException() {}

  RuntimeAttachException(String message) {
    super(message);
  }

  RuntimeAttachException(String message, Throwable cause) {
    super(message, cause);
  }
}
