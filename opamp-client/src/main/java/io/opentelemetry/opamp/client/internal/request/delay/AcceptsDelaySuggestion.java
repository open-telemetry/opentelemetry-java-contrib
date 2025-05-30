package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;

/**
 * A {@link PeriodicDelay} implementation that wants to accept delay time suggestions, as explained
 * <a
 * href="https://github.com/open-telemetry/opamp-spec/blob/main/specification.md#throttling">here</a>,
 * must implement this interface.
 */
public interface AcceptsDelaySuggestion {
  void suggestDelay(Duration delay);
}
