/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Utility class to use the {@link AutoConfiguredOpenTelemetrySdk}. */
public class AutoConfigureUtil2 {

  private AutoConfigureUtil2() {}

  /**
   * Returns the {@link Resource} that was autoconfigured.
   *
   * <p>Inspired by {@link
   * io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil#getConfig(AutoConfiguredOpenTelemetrySdk)}
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
