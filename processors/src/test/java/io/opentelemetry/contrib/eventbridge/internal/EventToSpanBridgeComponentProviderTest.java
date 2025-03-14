/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.eventbridge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EventToSpanBridgeComponentProviderTest {

  @Test
  void endToEnd() {
    String yaml =
        "file_format: 0.3\n"
            + "logger_provider:\n"
            + "  processors:\n"
            + "    - event_to_span_event_bridge:\n";

    OpenTelemetrySdk openTelemetrySdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(openTelemetrySdk.getSdkLoggerProvider().toString())
        .matches("SdkLoggerProvider\\{.*logRecordProcessor=EventToSpanEventBridge\\{}.*}");
  }
}
