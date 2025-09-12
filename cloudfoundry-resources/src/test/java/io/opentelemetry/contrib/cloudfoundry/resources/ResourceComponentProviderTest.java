/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {
  @Test
  void endToEnd() {
    assertThat(
            AutoConfiguredOpenTelemetrySdk.initialize()
                .getOpenTelemetrySdk()
                .getSdkTracerProvider())
        .extracting("sharedState")
        .extracting("resource")
        .extracting(
            "attributes",
            new InstanceOfAssertFactory<>(Attributes.class, OpenTelemetryAssertions::assertThat))
        .containsEntry("cloudfoundry.app.name", "cf-app-name");
  }
}
