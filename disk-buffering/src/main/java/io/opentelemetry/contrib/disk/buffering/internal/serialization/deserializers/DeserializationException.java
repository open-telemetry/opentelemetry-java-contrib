/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import java.io.IOException;

@SuppressWarnings("serial")
public class DeserializationException extends IOException {
  public DeserializationException(Throwable cause) {
    super(cause);
  }

  public DeserializationException(String message) {
    super(message);
  }
}
