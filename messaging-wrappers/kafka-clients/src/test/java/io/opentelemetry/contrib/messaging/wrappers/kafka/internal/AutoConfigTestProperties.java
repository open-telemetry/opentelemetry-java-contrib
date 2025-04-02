package io.opentelemetry.contrib.messaging.wrappers.kafka.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;

public class AutoConfigTestProperties implements AutoCloseable {

  public AutoConfigTestProperties() {
    put("otel.java.global-autoconfigure.enabled", "true");
    put("otel.traces.exporter", "logging");
    put("otel.metrics.exporter", "logging");
    put("otel.logs.exporter", "logging");
  }

  private final Map<String, String> originalValues = new HashMap<>();

  @CanIgnoreReturnValue
  public AutoConfigTestProperties put(String key, String value) {
    if (!originalValues.containsKey(key)) {
      originalValues.put(key, System.getProperty(key));
    }
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
    return this;
  }

  @Override
  public void close() {
    for (String key : originalValues.keySet()) {
      String value = originalValues.get(key);
      if (value == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, value);
      }
    }
  }
}
