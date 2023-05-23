/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** A builder for {@link AwsXrayRemoteSampler}. */
public final class AwsXrayRemoteSamplerBuilder {

  private static final String DEFAULT_ENDPOINT = "http://localhost:2000";
  private static final long DEFAULT_POLLING_INTERVAL_SECS = 300;

  private final Resource resource;

  private Clock clock = Clock.getDefault();
  private String endpoint = DEFAULT_ENDPOINT;
  @Nullable private Sampler initialSampler;
  private long pollingIntervalNanos = TimeUnit.SECONDS.toNanos(DEFAULT_POLLING_INTERVAL_SECS);

  AwsXrayRemoteSamplerBuilder(Resource resource) {
    this.resource = resource;
  }

  /**
   * Sets the endpoint for the TCP proxy to connect to. This is the address to the port on the
   * OpenTelemetry Collector configured for proxying X-Ray sampling requests. If unset, defaults to
   * {@value DEFAULT_ENDPOINT}.
   */
  @CanIgnoreReturnValue
  public AwsXrayRemoteSamplerBuilder setEndpoint(String endpoint) {
    requireNonNull(endpoint, "endpoint");
    this.endpoint = endpoint;
    return this;
  }

  /**
   * Sets the polling interval for configuration updates. If unset, defaults to {@value
   * DEFAULT_POLLING_INTERVAL_SECS}s. Must be positive.
   */
  @CanIgnoreReturnValue
  public AwsXrayRemoteSamplerBuilder setPollingInterval(Duration delay) {
    requireNonNull(delay, "delay");
    return setPollingInterval(delay.toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Sets the polling interval for configuration updates. If unset, defaults to {@value
   * DEFAULT_POLLING_INTERVAL_SECS}s. Must be positive.
   */
  @CanIgnoreReturnValue
  public AwsXrayRemoteSamplerBuilder setPollingInterval(long delay, TimeUnit unit) {
    requireNonNull(unit, "unit");
    if (delay < 0) {
      throw new IllegalArgumentException("delay must be non-negative");
    }
    pollingIntervalNanos = unit.toNanos(delay);
    return this;
  }

  /**
   * Sets the initial sampler that is used before sampling configuration is obtained. If unset,
   * defaults to a parent-based always-on sampler.
   */
  @CanIgnoreReturnValue
  public AwsXrayRemoteSamplerBuilder setInitialSampler(Sampler initialSampler) {
    requireNonNull(initialSampler, "initialSampler");
    this.initialSampler = initialSampler;
    return this;
  }

  /**
   * Sets the {@link Clock} used for time measurements for sampling, such as rate limiting or quota
   * expiry.
   */
  @CanIgnoreReturnValue
  public AwsXrayRemoteSamplerBuilder setClock(Clock clock) {
    requireNonNull(clock, "clock");
    this.clock = clock;
    return this;
  }

  /** Returns a {@link AwsXrayRemoteSampler} with the configuration of this builder. */
  public AwsXrayRemoteSampler build() {
    Sampler initialSampler = this.initialSampler;
    if (initialSampler == null) {
      initialSampler =
          Sampler.parentBased(
              new OrElseSampler(
                  new RateLimitingSampler(1, clock), Sampler.traceIdRatioBased(0.05)));
    }
    return new AwsXrayRemoteSampler(
        resource, clock, endpoint, initialSampler, pollingIntervalNanos);
  }
}
