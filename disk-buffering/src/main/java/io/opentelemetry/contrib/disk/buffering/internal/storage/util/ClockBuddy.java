/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.util;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import io.opentelemetry.sdk.common.Clock;

public class ClockBuddy {

  private ClockBuddy() {}

  /** Returns the current time in millis from the given clock */
  public static final long nowMillis(Clock clock) {
    return NANOSECONDS.toMillis(clock.now());
  }
}
