package io.opentelemetry.contrib.disk.buffer.internal.serialization;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.runtime.Settings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class JsonSerializer {

  private static final DslJson<Object> dslJson =
      new DslJson<>(Settings.basicSetup().skipDefaultValues(true));

  private JsonSerializer() {}

  public static <T> JsonReader.ReadObject<T> tryFindReader(Class<T> manifest) {
    return dslJson.tryFindReader(manifest);
  }

  public static <T> JsonWriter.WriteObject<T> tryFindWriter(Class<T> manifest) {
    return dslJson.tryFindWriter(manifest);
  }

  public static <T> T deserialize(Class<T> type, byte[] value) throws IOException {
    try (ByteArrayInputStream in = new ByteArrayInputStream(value)) {
      return dslJson.deserialize(type, in);
    }
  }

  public static byte[] serialize(Object object) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      dslJson.serialize(object, out);
      return out.toByteArray();
    }
  }
}
