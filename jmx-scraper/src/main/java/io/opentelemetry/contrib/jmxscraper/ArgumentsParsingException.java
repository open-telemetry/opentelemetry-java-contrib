/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

public class ArgumentsParsingException extends Exception {
  private static final long serialVersionUID = 0L;

  public ArgumentsParsingException(String msg) {
    super(msg);
  }
}
