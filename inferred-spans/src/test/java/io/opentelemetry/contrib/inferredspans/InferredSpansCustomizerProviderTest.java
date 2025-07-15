/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class InferredSpansCustomizerProviderTest {

  @Test
  void declarativeConfig() {
    String yaml =
        "file_format: 0.4\n"
            + "tracer_provider:\n"
            + "instrumentation/development:\n"
            + "  java:\n"
            + "    inferred_spans:\n"
            + "      enabled: true\n";

    OpenTelemetryConfigurationModel model =
        DeclarativeConfiguration.parse(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    new InferredSpansCustomizerProvider()
        .customize(
            c -> {
              OpenTelemetryConfigurationModel configurationModel = c.apply(model);
              assertThat(configurationModel.toString())
                  .matches(
                      ".*SpanProcessorModel@.{8}\\[batch=<null>,simple=<null>,additionalProperties=\\{inferred_spans=null}.*");
            });
  }
}
