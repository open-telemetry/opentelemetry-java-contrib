/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Temporary resource access until the SDK offers a supported resource API. */
final class SdkResourceAccess {
  private static final Logger logger = Logger.getLogger(SdkResourceAccess.class.getName());

  @Nullable
  static Resource getResource(OpenTelemetrySdk sdk) {
    try {
      // This declarative path installs a policy sampler, so the tracer provider is the configured
      // signal provider. Do not substitute another signal's potentially different/default resource.
      Field field = SdkTracerProvider.class.getDeclaredField("sharedState");
      field.setAccessible(true);
      Object state = field.get(sdk.getSdkTracerProvider());
      Method getter = field.getType().getDeclaredMethod("getResource");
      getter.setAccessible(true);
      return (Resource) getter.invoke(state);
    } catch (ReflectiveOperationException | RuntimeException e) {
      logger.log(
          Level.WARNING,
          "Cannot access the SDK resource; falling back to legacy OpAMP identity properties. "
              + "Ensure they match the SDK service name and deployment environment.",
          e);
      return null;
    }
  }

  private SdkResourceAccess() {}
}
