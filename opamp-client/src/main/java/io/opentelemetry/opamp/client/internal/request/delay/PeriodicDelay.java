package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;

public interface PeriodicDelay {
  static PeriodicDelay ofFixedDuration(Duration duration) {
    return new FixedPeriodicDelay(duration);
  }

  Duration getNextDelay();

  void reset();
}
