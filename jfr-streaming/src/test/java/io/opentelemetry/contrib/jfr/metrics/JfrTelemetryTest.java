/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JfrTelemetryTest {

  @RegisterExtension LogCapturer logs = LogCapturer.create().captureForType(JfrTelemetry.class);

  private InMemoryMetricReader reader;
  private OpenTelemetrySdk sdk;

  @BeforeEach
  void setup() {
    reader = InMemoryMetricReader.create();
    sdk =
        OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
  }

  @AfterEach
  void tearDown() {
    sdk.getSdkMeterProvider().close();
  }

  @Test
  void create() {
    try (JfrTelemetry unused = JfrTelemetry.create(sdk)) {
      assertThat(logs.getEvents()).hasSize(1);
      logs.assertContains("Starting JfrTelemetry");

      await().untilAsserted(() -> assertThat(reader.collectAllMetrics()).isNotEmpty());
    }
  }

  @Test
  void close() {
    try (JfrTelemetry jfrTelemetry = JfrTelemetry.create(sdk)) {
      // Track whether RecordingStream has been closed
      AtomicBoolean recordingStreamClosed = new AtomicBoolean(false);
      jfrTelemetry.getRecordingStream().onClose(() -> recordingStreamClosed.set(true));

      jfrTelemetry.close();
      logs.assertContains("Closing JfrTelemetry");
      logs.assertDoesNotContain("JfrTelemetry is already closed");
      assertThat(recordingStreamClosed.get()).isTrue();

      jfrTelemetry.close();
      logs.assertContains("JfrTelemetry is already closed");
    }
  }
}
