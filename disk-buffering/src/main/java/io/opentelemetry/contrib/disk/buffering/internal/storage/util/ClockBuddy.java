/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.util;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import io.opentelemetry.sdk.common.Clock;

/**
 * Utility methods for working with {@link Clock}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class ClockBuddy {

  private ClockBuddy() {}

  /** Returns the current time in millis from the given clock */
  public static final long nowMillis(Clock clock) {
    return NANOSECONDS.toMillis(clock.now());
  }
}
