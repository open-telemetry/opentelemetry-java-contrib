/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.internal.ExtendedOpenTelemetrySdk;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {

  @Test
  void endToEnd() {
    ExtendedOpenTelemetrySdk sdk =
        (ExtendedOpenTelemetrySdk)
            AutoConfiguredOpenTelemetrySdk.builder().build().getOpenTelemetrySdk();

    assertThat(sdk.getSdkResourceProvider().getResource().getAttributes())
        .containsEntry(
            CloudIncubatingAttributes.CLOUD_PROVIDER,
            CloudIncubatingAttributes.CloudProviderIncubatingValues.AZURE);
  }
}
