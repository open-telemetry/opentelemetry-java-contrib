/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;

/**
 * Provides the delay to wait between requests.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface PeriodicDelay {
  static PeriodicDelay ofFixedDuration(Duration duration) {
    return new FixedPeriodicDelay(duration);
  }

  Duration getNextDelay();

  void reset();
}
