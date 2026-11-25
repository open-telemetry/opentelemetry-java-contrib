/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisabledOnOs(WINDOWS) // Uses async-profiler, which is not supported on Windows
class InferredSpansSpanProcessorProviderTest {

  private ProfilerTestSetup setup;

  @BeforeEach
  void setUp() {
    setup = ProfilerTestSetup.create(c -> {});
  }

  @AfterEach
  void tearDown() {
    if (setup != null) {
      setup.close();
    }
    InferredSpans.setInstance(null);
  }

  @Test
  void declarativeConfig() {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        backup_diagnostic_files: true\n";

    try (OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk()) {
      assertThat(sdk)
          .extracting("tracerProvider")
          .extracting("delegate")
          .extracting("sharedState")
          .extracting("activeSpanProcessor")
          .extracting("profiler")
          .extracting("config")
          .extracting("backupDiagnosticFiles")
          .isEqualTo(true);
    }
  }

  @Test
  void declarativeConfigMapsEveryProperty() {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        enabled: true\n"
            + "        logging_enabled: false\n"
            + "        backup_diagnostic_files: true\n"
            + "        safe_mode: 7\n"
            + "        post_processing_enabled: false\n"
            + "        sampling_interval: 7\n"
            + "        min_duration: 9\n"
            + "        included_classes: included.one.*,included.two.*\n"
            + "        excluded_classes: excluded.one.*,excluded.two.*\n"
            + "        interval: 11000\n"
            + "        duration: 13000\n"
            + "        lib_directory: /tmp/inferred-spans-test\n"
            + "        parent_override_handler: "
            + TestParentOverrideHandler.class.getName()
            + "\n";

    try (OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk()) {
      assertThat(sdk)
          .extracting("tracerProvider")
          .extracting("delegate")
          .extracting("sharedState")
          .extracting("activeSpanProcessor")
          .extracting("profiler")
          .extracting("config")
          .satisfies(config -> {
            assertThat(config).extracting("profilerLoggingEnabled").isEqualTo(false);
            assertThat(config).extracting("backupDiagnosticFiles").isEqualTo(true);
            assertThat(config).extracting("asyncProfilerSafeMode").isEqualTo(7);
            assertThat(config).extracting("postProcessingEnabled").isEqualTo(false);
            assertThat(config).extracting("samplingInterval").isEqualTo(Duration.ofMillis(7));
            assertThat(config)
                .extracting("inferredSpansMinDuration")
                .isEqualTo(Duration.ofMillis(9));
            assertThat(config)
                .extracting("includedClasses")
                .satisfies(v -> assertThat((List<?>) v).hasSize(2));
            assertThat(config)
                .extracting("excludedClasses")
                .satisfies(v -> assertThat((List<?>) v).hasSize(2));
            assertThat(config).extracting("profilerInterval").isEqualTo(Duration.ofSeconds(11));
            assertThat(config).extracting("profilingDuration").isEqualTo(Duration.ofSeconds(13));
            assertThat(config)
                .extracting("profilerLibDirectory")
                .isEqualTo("/tmp/inferred-spans-test");
            assertThat(config)
                .extracting("parentOverrideHandler")
                .isInstanceOf(TestParentOverrideHandler.class);
          });
    }
  }

  @Test
  void declarativeConfigDisabled() {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        enabled: false\n";

    try (OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk()) {
      assertThat(sdk)
          .extracting("tracerProvider")
          .extracting("delegate")
          .extracting("sharedState")
          .extracting("activeSpanProcessor")
          .satisfies(p ->
              assertThat(p.getClass().getName())
                  .isEqualTo("io.opentelemetry.sdk.trace.NoopSpanProcessor"));
    }
  }

  @Test
  void declarativeConfigNullDurationUsesDefault() {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        min_duration:\n";

    try (OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk()) {
      assertThat(sdk)
          .extracting("tracerProvider")
          .extracting("delegate")
          .extracting("sharedState")
          .extracting("activeSpanProcessor")
          .extracting("profiler")
          .extracting("config")
          .extracting("inferredSpansMinDuration")
          .isEqualTo(Duration.ZERO);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"sampling_interval", "min_duration", "interval", "duration"})
  void declarativeConfigRejectsNonIntegerDuration(String key) {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        "
            + key
            + ": 10ms\n";

    assertThatThrownBy(() ->
            DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))))
        .hasStackTraceContaining(
            "inferred_spans/development ."
                + key
                + " must be an integer number of milliseconds, got: 10ms");
  }

  public static class TestParentOverrideHandler implements BiConsumer<SpanBuilder, SpanContext> {
    @Override
    public void accept(SpanBuilder spanBuilder, SpanContext spanContext) {}
  }
}
