/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InferredSpansCustomizerProviderTest {

  private static final String FQDN =
      "spanProcessor=io.opentelemetry.contrib.inferredspans.InferredSpansProcessor";

  @Test
  void enabled() {
    assertThat(create("")).contains(FQDN);
  }

  @Test
  void disabled() {
    assertThat(create("enabled: false")).doesNotContain(FQDN);
  }

  private static String create(String enabled) {
    String yaml =
        "file_format: 0.4\n"
            + "tracer_provider:\n"
            + "  processors:\n"
            + "    - inferred_spans:\n"
            + "        "
            + enabled
            + "\n";
    return DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .toString();
  }
}
