/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.contrib.inferredspans.internal.InferredSpansConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

@DisabledOnOs(WINDOWS) // Uses async-profiler, which is not supported on Windows
class InferredSpansConfigTest {

  @AfterEach
  void tearDown() {
    InferredSpans.setInstance(null);
  }

  @Test
  void createDeclarativeConfigMapsEveryProperty() {
    Map<String, String> configMap = new LinkedHashMap<>();
    configMap.put("otel.inferred.spans.enabled", "false");
    configMap.put("otel.inferred.spans.logging.enabled", "false");
    configMap.put("otel.inferred.spans.backup.diagnostic.files", "true");
    configMap.put("otel.inferred.spans.safe.mode", "7");
    configMap.put("otel.inferred.spans.post.processing.enabled", "false");
    configMap.put("otel.inferred.spans.sampling.interval", "7ms");
    configMap.put("otel.inferred.spans.min.duration", "9ms");
    configMap.put("otel.inferred.spans.included.classes", "included.one.*,included.two.*");
    configMap.put("otel.inferred.spans.excluded.classes", "excluded.one.*,excluded.two.*");
    configMap.put("otel.inferred.spans.interval", "11s");
    configMap.put("otel.inferred.spans.duration", "13s");
    configMap.put("otel.inferred.spans.lib.directory", "/tmp/inferred-spans-test");
    configMap.put(
        "otel.inferred.spans.parent.override.handler", TestParentOverrideHandler.class.getName());

    DefaultConfigProperties configProperties = DefaultConfigProperties.createFromMap(configMap);
    DeclarativeConfigProperties declarativeConfig =
        InferredSpansConfig.createDeclarativeConfig(configProperties);
    InferredSpansProcessor processor =
        (InferredSpansProcessor) InferredSpansConfig.createSpanProcessor(declarativeConfig);

    InferredSpansConfiguration configuration = getConfiguration(processor);

    assertThat(configuration.isBackupDiagnosticFiles()).isTrue();
    assertThat(InferredSpansConfig.isEnabled(declarativeConfig)).isFalse();
    assertThat(configuration.isProfilingLoggingEnabled()).isFalse();
    assertThat(configuration.isBackupDiagnosticFiles()).isTrue();
    assertThat(configuration.getAsyncProfilerSafeMode()).isEqualTo(7);
    assertThat(configuration.isPostProcessingEnabled()).isFalse();
    assertThat(configuration.getSamplingInterval()).isEqualTo(Duration.ofMillis(7));
    assertThat(configuration.getInferredSpansMinDuration()).isEqualTo(Duration.ofMillis(9));
    assertThat(configuration.getIncludedClasses()).hasSize(2);
    assertThat(configuration.getExcludedClasses()).hasSize(2);
    assertThat(configuration.getProfilingInterval()).isEqualTo(Duration.ofSeconds(11));
    assertThat(configuration.getProfilingDuration()).isEqualTo(Duration.ofSeconds(13));
    assertThat(configuration.getProfilerLibDirectory()).isEqualTo("/tmp/inferred-spans-test");
    assertThat(configuration.getParentOverrideHandler())
        .isInstanceOf(TestParentOverrideHandler.class);

    processor.shutdown();
  }

  private static InferredSpansConfiguration getConfiguration(InferredSpansProcessor processor) {
    try {
      Field field = InferredSpansProcessor.class.getDeclaredField("config");
      field.setAccessible(true);
      return (InferredSpansConfiguration) field.get(processor);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  public static class TestParentOverrideHandler implements BiConsumer<SpanBuilder, SpanContext> {
    @Override
    public void accept(SpanBuilder spanBuilder, SpanContext spanContext) {}
  }
}
