package io.opentelemetry.opamp.client.internal.request.service;

import io.opentelemetry.opamp.client.internal.request.delay.AcceptsDelaySuggestion;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import java.time.Duration;

public class PeriodicDelayWithSuggestion implements PeriodicDelay, AcceptsDelaySuggestion {
  private final Duration initialDelay;
  private Duration currentDelay;

  public PeriodicDelayWithSuggestion(Duration initialDelay) {
    this.initialDelay = initialDelay;
    currentDelay = initialDelay;
  }

  @Override
  public void suggestDelay(Duration delay) {
    currentDelay = delay;
  }

  @Override
  public Duration getNextDelay() {
    return currentDelay;
  }

  @Override
  public void reset() {
    currentDelay = initialDelay;
  }
}
