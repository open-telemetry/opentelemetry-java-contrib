/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayLambdaPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AwsComponentProviderTest {

  @Test
  void endToEnd() {
    String yaml =
        "file_format: 1.0-rc.1\n"
            + "propagator:\n"
            + "  composite:\n"
            + "    - xray:\n"
            + "    - xray-lambda:\n";

    OpenTelemetrySdk openTelemetrySdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    TextMapPropagator expectedPropagator =
        TextMapPropagator.composite(
            AwsXrayPropagator.getInstance(), AwsXrayLambdaPropagator.getInstance());
    assertThat(openTelemetrySdk.getPropagators().getTextMapPropagator().toString())
        .isEqualTo(expectedPropagator.toString());
  }
}
