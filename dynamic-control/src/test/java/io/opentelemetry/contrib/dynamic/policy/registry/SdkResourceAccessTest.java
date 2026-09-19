/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;

class SdkResourceAccessTest {
  @Test
  void readsTracerProviderResource() {
    Resource resource = Resource.builder().put("service.name", "resolved-service").build();
    try (OpenTelemetrySdk sdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().setResource(resource).build())
            .build()) {
      assertThat(SdkResourceAccess.getResource(sdk)).isSameAs(resource);
    }
  }

  @Test
  @SuppressWarnings("CannotMockMethod") // mockito-inline supports final SDK methods.
  void returnsNullWhenResourceAccessFails() {
    OpenTelemetrySdk sdk = mock(OpenTelemetrySdk.class);
    when(sdk.getSdkTracerProvider()).thenThrow(new IllegalStateException("unavailable provider"));
    assertThat(SdkResourceAccess.getResource(sdk)).isNull();
  }
}
