/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.Test;

class DynamicControlAutoConfigurationTest {

  @Test
  void testCustomize() {
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    config.customize(customizer);

    // The customize method should not throw and should be callable
    // Logging is tested manually or via integration tests
  }

  @Test
  void testOrder() {
    //This is a placeholder test, just to have something
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    // Default order should be 0
    assertThat(config.order()).isEqualTo(0);
  }
}
