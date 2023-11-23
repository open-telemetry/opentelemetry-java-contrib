/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppServerServiceNameProviderTest {

  @Mock ServiceNameDetector detector;

  @Test
  void createResource_empty() {
    AppServerServiceNameProvider provider = new AppServerServiceNameProvider(detector);

    Resource result = provider.createResource(null);
    assertThat(result).isSameAs(Resource.empty());
  }

  @Test
  void createResource() throws Exception {

    when(detector.detect()).thenReturn("mythinger");

    AppServerServiceNameProvider provider = new AppServerServiceNameProvider(detector);

    Resource result = provider.createResource(null);
    assertThat(result.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("mythinger");
  }

  @Test
  void detectorThrows() throws Exception {
    when(detector.detect()).thenThrow(new RuntimeException("OUCH IT HURTS!"));

    AppServerServiceNameProvider provider = new AppServerServiceNameProvider(detector);

    Resource result = provider.createResource(null);
    assertThat(result).isSameAs(Resource.empty());
  }

  @Test
  void shouldApply() {
    Resource existing =
        Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME.getKey(), "unknown_service:java")
            .build();

    ConfigProperties config = mock(ConfigProperties.class);

    AppServerServiceNameProvider provider = new AppServerServiceNameProvider();

    boolean result = provider.shouldApply(config, existing);
    assertThat(result).isTrue();
  }

  @Test
  void shouldApply_serviceNameAlreadySetInConfig() {
    Resource existing = Resource.builder().build();

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("otel.service.name")).thenReturn("trev");

    AppServerServiceNameProvider provider = new AppServerServiceNameProvider();

    boolean result = provider.shouldApply(config, existing);
    assertThat(result).isFalse();
  }

  @Test
  void shouldApply_serviceNameAlreadyInResource() {
    Resource existing =
        Resource.builder().put(ResourceAttributes.SERVICE_NAME.getKey(), "shemp").build();

    ConfigProperties config = mock(ConfigProperties.class);

    AppServerServiceNameProvider provider = new AppServerServiceNameProvider();

    boolean result = provider.shouldApply(config, existing);
    assertThat(result).isFalse();
  }
}
