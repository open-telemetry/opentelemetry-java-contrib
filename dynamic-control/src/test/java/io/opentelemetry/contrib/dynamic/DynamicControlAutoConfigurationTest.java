/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.Test;

class DynamicControlAutoConfigurationTest {

  @Test
  void testCustomize() {
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    config.customize(customizer);

    verify(customizer).addPropertiesCustomizer(any());
  }

  @Test
  void testOrder() {
    // This is a placeholder test, just to have something
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    // Default order should be 0
    assertThat(config.order()).isEqualTo(0);
  }
}
