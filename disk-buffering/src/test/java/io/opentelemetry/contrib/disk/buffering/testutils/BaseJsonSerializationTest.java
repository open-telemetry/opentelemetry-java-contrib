/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.testutils;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.JsonSerializer;
import java.io.IOException;

public abstract class BaseJsonSerializationTest<T> {
  protected byte[] serialize(T item) {
    try {
      return JsonSerializer.serialize(item);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected T deserialize(byte[] json) {
    try {
      return JsonSerializer.deserialize(getTargetClass(), json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract Class<T> getTargetClass();
}
