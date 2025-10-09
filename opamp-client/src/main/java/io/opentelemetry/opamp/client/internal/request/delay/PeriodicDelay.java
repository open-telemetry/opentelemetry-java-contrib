/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;

public interface PeriodicDelay {
  static PeriodicDelay ofFixedDuration(Duration duration) {
    return new FixedPeriodicDelay(duration);
  }

  Duration getNextDelay();

  void reset();
}
