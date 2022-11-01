/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
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
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal(false).build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verify(logWarn).accept(anyString());
  }

  @Test
  void shouldNotLogWarnWhenServiceNameIsConfigured() {
    // given
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> Map.of("otel.service.name", "test"))
            .build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }

  @Test
  void shouldNotLogWarnWhenResourceAttributeIsConfigured() {
    // given
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> Map.of("otel.resource.attributes", "service.name=test"))
            .build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }
}
