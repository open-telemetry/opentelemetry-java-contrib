/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

/**
 * Exception indicating something is wrong with the provided arguments or reading the configuration
 * from them
 */
public class InvalidArgumentException extends Exception {

  private static final long serialVersionUID = 0L;

  public InvalidArgumentException(String msg) {
    super(msg);
  }

  public InvalidArgumentException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
