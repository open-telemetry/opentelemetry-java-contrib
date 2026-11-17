/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import java.io.IOException;

/**
 * Exception thrown when serialized signal data cannot be deserialized.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@SuppressWarnings("serial")
public class DeserializationException extends IOException {
  public DeserializationException(Throwable cause) {
    super(cause);
  }
}
