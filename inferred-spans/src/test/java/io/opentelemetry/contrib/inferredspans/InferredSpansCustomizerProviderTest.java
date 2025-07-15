/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InferredSpansCustomizerProviderTest {

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

    assertThat(sdk.toString())
        .contains("spanProcessor=io.opentelemetry.contrib.inferredspans.InferredSpansProcessor");
  }
}
