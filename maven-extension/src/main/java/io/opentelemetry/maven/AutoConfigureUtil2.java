/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.resources.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AutoConfigureUtil2 {

  private AutoConfigureUtil2() {}

  /**
   * Returns the {@link Resource} that was auto-configured.
   *
   * @see AutoConfigureUtil#getConfig(AutoConfiguredOpenTelemetrySdk)
   */
  public static Resource getResource(
      AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    try {
      Method method = AutoConfiguredOpenTelemetrySdk.class.getDeclaredMethod("getResource");
      method.setAccessible(true);
      return (Resource) method.invoke(autoConfiguredOpenTelemetrySdk);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Error calling getResource on AutoConfiguredOpenTelemetrySdk", e);
    }
  }
}
