/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResourceHolder}. Note that there isn't a great way to test the "default"
 * fallback logic, as when the test suite is run, the customize logic appears to be invoked.
 */
public class ResourceHolderTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testCustomized() {
    Resource customizedResource = Resource.create(Attributes.empty());
    AutoConfigurationCustomizer mockCustomizer = mock(AutoConfigurationCustomizer.class);
    ResourceHolder resourceHolder = new ResourceHolder();
    when(mockCustomizer.addResourceCustomizer(any()))
        .thenAnswer(
            invocation -> {
              BiFunction<Resource, ConfigProperties, Resource> biFunction =
                  (BiFunction<Resource, ConfigProperties, Resource>) invocation.getArguments()[0];
              assertThat(biFunction.apply(customizedResource, null)).isEqualTo(customizedResource);
              return mockCustomizer;
            });
    resourceHolder.customize(mockCustomizer);
    assertThat(ResourceHolder.getResource()).isEqualTo(customizedResource);
  }
}
