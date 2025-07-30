/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Defaults to an exponential backoff strategy, unless a delay is suggested. */
public final class RetryPeriodicDelay implements PeriodicDelay, AcceptsDelaySuggestion {
  private final ExponentialBackoffPeriodicDelay exponentialBackoff;
  private final AtomicReference<PeriodicDelay> currentDelay;

  public static RetryPeriodicDelay create(Duration initialDelay) {
    return new RetryPeriodicDelay(new ExponentialBackoffPeriodicDelay(initialDelay));
  }

  private RetryPeriodicDelay(ExponentialBackoffPeriodicDelay exponentialBackoff) {
    this.exponentialBackoff = exponentialBackoff;
    currentDelay = new AtomicReference<>(exponentialBackoff);
  }

  @Override
  public void suggestDelay(Duration delay) {
    currentDelay.set(PeriodicDelay.ofFixedDuration(delay));
  }

  @Override
  public Duration getNextDelay() {
    return Objects.requireNonNull(currentDelay.get()).getNextDelay();
  }

  @Override
  public void reset() {
    exponentialBackoff.reset();
    currentDelay.set(exponentialBackoff);
  }
}
