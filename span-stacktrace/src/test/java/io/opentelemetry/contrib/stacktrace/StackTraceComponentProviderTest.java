/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StackTraceComponentProviderTest {
  @Test
  void endToEnd() {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - stacktrace/development: \n"
            + "        min_duration: 100\n"
            + "        filter: io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessorTest$YesPredicate\n";

    OpenTelemetrySdk openTelemetrySdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk();

    assertThat(openTelemetrySdk.getSdkTracerProvider().toString())
        .contains(
            String.format(
                Locale.ROOT,
                "StackTraceSpanProcessor{minSpanDurationNanos=%d, "
                    + "filterPredicate=io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessorTest$YesPredicate",
                TimeUnit.MILLISECONDS.toNanos(100)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "min_duration:"})
  void unsetMinDurationUsesDefault(String minDurationLine) {
    OpenTelemetrySdk openTelemetrySdk = createSdk(minDurationLine);

    assertThat(openTelemetrySdk.getSdkTracerProvider().toString())
        .contains("minSpanDurationNanos=" + TimeUnit.MILLISECONDS.toNanos(5) + ",");
  }

  @ParameterizedTest
  @ValueSource(strings = {"'10ms'", "10ms", "'10'", "10.5"})
  void nonIntegerMinDurationIsRejected(String value) {
    assertThatThrownBy(() -> createSdk("min_duration: " + value))
        .hasStackTraceContaining(
            "stacktrace/development .min_duration must be an integer number of milliseconds");
  }

  private static OpenTelemetrySdk createSdk(String processorConfigLine) {
    String yaml =
        "file_format: '1.0'\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - stacktrace/development:\n"
            + "        "
            + processorConfigLine
            + "\n";
    return DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getSdk();
  }
}
