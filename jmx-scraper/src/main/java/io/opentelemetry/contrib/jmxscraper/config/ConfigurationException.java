/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

public class ConfigurationException extends Exception {
  private static final long serialVersionUID = 0L;

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(String message) {
    super(message);
  }
}
