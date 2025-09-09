/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BaggageLogRecordComponentProviderTest {

  @Test
  void declarativeConfig() {
    String yaml =
        "file_format: 1.0-rc.1\n"
            + "logger_provider:\n"
            + "  processors:\n"
            + "    - baggage:\n"
            + "        included: [foo]\n"
            + "        excluded: [bar]\n";

    OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(sdk).asString().contains("BaggageLogRecordProcessor");
  }
}
