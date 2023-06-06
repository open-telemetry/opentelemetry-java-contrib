package io.opentelemetry.contrib.exporters.storage.testutils;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseJsonSerializationTest<T> {
  private final DslJson<Object> dslJson = new DslJson<>(Settings.basicSetup());

  protected byte[] serialize(T item) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      dslJson.serialize(item, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected T deserialize(byte[] json) {
    try (ByteArrayInputStream in = new ByteArrayInputStream(json)) {
      return dslJson.deserialize(getTargetClass(), in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract Class<T> getTargetClass();
}
