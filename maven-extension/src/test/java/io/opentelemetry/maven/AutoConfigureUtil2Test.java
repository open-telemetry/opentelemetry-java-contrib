/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class AutoConfigureUtil2Test {

  /**
   * Verify the reflection call works with the current version of AutoConfiguredOpenTelemetrySdk.
   *
   * @throws NoSuchMethodException if the method does not exist
   */
  @Test
  void test_getResource() throws NoSuchMethodException {
    Method method = AutoConfiguredOpenTelemetrySdk.class.getDeclaredMethod("getResource");
    method.setAccessible(true);
  }
}
