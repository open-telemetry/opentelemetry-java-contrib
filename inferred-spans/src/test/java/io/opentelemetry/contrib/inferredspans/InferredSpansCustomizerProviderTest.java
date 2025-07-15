/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.inferredspans.internal.ProfilingActivationListener;
import io.opentelemetry.contrib.inferredspans.internal.util.OtelReflectionUtils;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InferredSpansCustomizerProviderTest {

  @BeforeEach
  @AfterEach
  public void resetGlobalOtel() {
    ProfilingActivationListener.ensureInitialized();
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  void declarativeConfig() {
    String yaml =
        "file_format: 0.4\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans:\n"
            + "        backup:\n"
            + "          diagnostic:\n"
            + "            files: true\n";

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
}
