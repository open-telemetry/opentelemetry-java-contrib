/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal;

import io.micrometer.core.instrument.Tag;

/**
 * Constants for common Micrometer {@link Tag} names for the OpenTelemetry instrumentation scope.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class Constants {
  public static final String OTEL_INSTRUMENTATION_NAME = "otel.instrumentation.name";
  public static final String OTEL_INSTRUMENTATION_VERSION = "otel.instrumentation.version";
  public static final Tag UNKNOWN_INSTRUMENTATION_VERSION_TAG =
      Tag.of(OTEL_INSTRUMENTATION_VERSION, "unknown");

  private Constants() {}
}
