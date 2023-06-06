package io.opentelemetry.contrib.disk.buffer.testutils;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.JsonSerializer;
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
