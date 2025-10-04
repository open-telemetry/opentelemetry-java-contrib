/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {
  @Test
  void endToEnd() {
    assertThat(
            AutoConfiguredOpenTelemetrySdk.builder()
                .build()
                .getOpenTelemetrySdk()
                .getSdkTracerProvider())
        .extracting("sharedState")
        .extracting("resource")
        .extracting(
            "attributes",
            new InstanceOfAssertFactory<>(Attributes.class, OpenTelemetryAssertions::assertThat))
        .containsEntry(
            CloudIncubatingAttributes.CLOUD_PROVIDER,
            CloudIncubatingAttributes.CloudProviderIncubatingValues.AWS);
  }
}
