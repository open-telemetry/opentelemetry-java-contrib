/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import java.time.Duration;

public final class DurationUtil {
  private static final double NANOS_PER_MILLI = 1e6;

  /** Returns the duration as milliseconds, with fractional part included. */
  @SuppressWarnings("TimeUnitMismatch")
  public static double toMillis(Duration duration) {
    double epochSecs = (double) duration.getSeconds();
    return epochSecs + duration.getNano() / NANOS_PER_MILLI;
  }

  private DurationUtil() {}
}
