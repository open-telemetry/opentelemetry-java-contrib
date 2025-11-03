/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

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
        "file_format: 1.0-rc.1\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        backup_diagnostic_files: true\n";

    OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

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

  @Test
  void declarativeConfigDisabled() {
    String yaml =
        "file_format: 1.0-rc.1\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans/development:\n"
            + "        enabled: false\n";

    OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(sdk)
        .extracting("tracerProvider")
        .extracting("delegate")
        .extracting("sharedState")
        .extracting("activeSpanProcessor")
        .satisfies(
            p ->
                assertThat(p.getClass().getName())
                    .isEqualTo("io.opentelemetry.sdk.trace.NoopSpanProcessor"));
  }
}
