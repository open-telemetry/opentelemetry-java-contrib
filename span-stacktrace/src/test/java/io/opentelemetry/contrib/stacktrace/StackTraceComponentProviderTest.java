/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StackTraceComponentProviderTest {
  @Test
  void endToEnd() {
    String yaml =
        "file_format: 1.0-rc.1\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - experimental_stacktrace: \n"
            + "        min_duration: 100\n"
            + "        filter: io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessorTest$YesPredicate\n";

    OpenTelemetrySdk openTelemetrySdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(openTelemetrySdk.getSdkTracerProvider().toString())
        .contains(
            String.format(
                Locale.ROOT,
                "StackTraceSpanProcessor{minSpanDurationNanos=%d, "
                    + "filterPredicate=io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessorTest$YesPredicate",
                TimeUnit.MILLISECONDS.toNanos(100)));
  }
}
