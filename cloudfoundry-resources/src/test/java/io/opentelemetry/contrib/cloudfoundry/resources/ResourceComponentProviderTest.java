/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resources;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.internal.ExtendedOpenTelemetrySdk;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {
  @Test
  void endToEnd() {
    ExtendedOpenTelemetrySdk sdk =
        (ExtendedOpenTelemetrySdk)
            AutoConfiguredOpenTelemetrySdk.builder().build().getOpenTelemetrySdk();

    assertThat(sdk.getSdkResourceProvider().getResource().getAttributes())
        .containsEntry("cloudfoundry.app.name", "cf-app-name");
  }
}
