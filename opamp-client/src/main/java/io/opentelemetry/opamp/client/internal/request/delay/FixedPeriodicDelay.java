/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;

final class FixedPeriodicDelay implements PeriodicDelay {
  private final Duration duration;

  public FixedPeriodicDelay(Duration duration) {
    this.duration = duration;
  }

  @Override
  public Duration getNextDelay() {
    return duration;
  }

  @Override
  public void reset() {}
}
