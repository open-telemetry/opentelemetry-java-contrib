/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

public final class ConfigurationException extends RuntimeException {
  private static final long serialVersionUID = 0L;

  public ConfigurationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(final String message) {
    super(message);
  }
}
