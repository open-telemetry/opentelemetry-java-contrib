/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceNameCheckerTest {
  @Mock Consumer<String> logWarn;

  @Test
  void shouldLogWarnWhenNeitherServiceNameNorResourceAttributeIsConfigured() {
    // given
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal(false).build();

    ServiceNameChecker underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verify(logWarn).accept(anyString());
  }

  @Test
  void shouldNotLogWarnWhenServiceNameIsConfigured() {
    // given
    Map<String, String> properties = new HashMap<>();
    properties.put("otel.service.name", "test");
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> properties)
            .build();

    ServiceNameChecker underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }

  @Test
  void shouldNotLogWarnWhenResourceAttributeIsConfigured() {
    // given
    Map<String, String> properties = new HashMap<>();
    properties.put("otel.resource.attributes", "service.name=test");
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> properties)
            .build();

    ServiceNameChecker underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }
}
