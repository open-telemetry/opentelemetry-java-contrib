/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingRulesResponse.SamplingRuleRecord;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Remote sampler that gets sampling configuration from AWS X-Ray. */
public final class AwsXrayRemoteSampler implements Sampler, Closeable {

  static final long DEFAULT_TARGET_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10);

  private static final Logger logger = Logger.getLogger(AwsXrayRemoteSampler.class.getName());

  private final Resource resource;
  private final Clock clock;
  private final Sampler initialSampler;
  private final XraySamplerClient client;
  private final ScheduledExecutorService executor;
  // Unique per-sampler client ID, generated as a random string.
  private final String clientId;
  private final long pollingIntervalNanos;
  private final Iterator<Long> jitterNanos;

  @Nullable private volatile ScheduledFuture<?> pollFuture;
  @Nullable private volatile ScheduledFuture<?> fetchTargetsFuture;
  @Nullable private volatile GetSamplingRulesResponse previousRulesResponse;
  private volatile Sampler sampler;

  /**
   * Returns a {@link AwsXrayRemoteSamplerBuilder} with the given {@link Resource}. This {@link
   * Resource} should be the same as what the OpenTelemetry SDK is configured with.
   */
  // TODO(anuraaga): Deprecate after
  // https://github.com/open-telemetry/opentelemetry-specification/issues/1588
  public static AwsXrayRemoteSamplerBuilder newBuilder(Resource resource) {
    return new AwsXrayRemoteSamplerBuilder(resource);
  }

  AwsXrayRemoteSampler(
      Resource resource,
      Clock clock,
      String endpoint,
      Sampler initialSampler,
      long pollingIntervalNanos) {
    this.resource = resource;
    this.clock = clock;
    this.initialSampler = initialSampler;
    client = new XraySamplerClient(endpoint);
    executor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = Executors.defaultThreadFactory().newThread(runnable);
              try {
                t.setDaemon(true);
                t.setName("xray-rules-poller");
              } catch (SecurityException e) {
                // Well, we tried.
              }
              return t;
            });

    clientId = generateClientId();

    sampler = initialSampler;

    this.pollingIntervalNanos = pollingIntervalNanos;
    // Add ~1% of jitter
    jitterNanos = ThreadLocalRandom.current().longs(0, pollingIntervalNanos / 100).iterator();

    // Execute first update right away on the executor thread.
    executor.execute(this::getAndUpdateSampler);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "AwsXrayRemoteSampler{" + sampler.getDescription() + "}";
  }

  private void getAndUpdateSampler() {
    try {
      // No pagination support yet, or possibly ever.
      GetSamplingRulesResponse response =
          client.getSamplingRules(GetSamplingRulesRequest.create(null));
      if (!response.equals(previousRulesResponse)) {
        sampler =
            new XrayRulesSampler(
                clientId,
                resource,
                clock,
                initialSampler,
                response.getSamplingRules().stream()
                    .map(SamplingRuleRecord::getRule)
                    .collect(Collectors.toList()));
        previousRulesResponse = response;
        ScheduledFuture<?> existingFetchTargetsFuture = fetchTargetsFuture;
        if (existingFetchTargetsFuture != null) {
          existingFetchTargetsFuture.cancel(false);
        }
        fetchTargetsFuture =
            executor.schedule(
                this::fetchTargets, DEFAULT_TARGET_INTERVAL_NANOS, TimeUnit.NANOSECONDS);
      }
    } catch (Throwable t) {
      logger.log(Level.FINE, "Failed to update sampler", t);
    }
    scheduleSamplerUpdate();
  }

  private void scheduleSamplerUpdate() {
    long delay = pollingIntervalNanos + jitterNanos.next();
    pollFuture = executor.schedule(this::getAndUpdateSampler, delay, TimeUnit.NANOSECONDS);
  }

  /**
   * returns the duration until the next scheduled sampler update or null if no next update is
   * scheduled yet.
   *
   * <p>only used for testing.
   */
  @Nullable
  Duration getNextSamplerUpdateScheduledDuration() {
    ScheduledFuture<?> pollFuture = this.pollFuture;
    if (pollFuture == null) {
      return null;
    }
    return Duration.ofNanos(pollFuture.getDelay(TimeUnit.NANOSECONDS));
  }

  private void fetchTargets() {
    if (!(sampler instanceof XrayRulesSampler)) {
      throw new IllegalStateException("Programming bug.");
    }

    XrayRulesSampler xrayRulesSampler = (XrayRulesSampler) sampler;
    try {
      Date now = Date.from(Instant.ofEpochSecond(0, clock.now()));
      List<SamplingStatisticsDocument> statistics = xrayRulesSampler.snapshot(now);
      Set<String> requestedTargetRuleNames =
          statistics.stream()
              .map(SamplingStatisticsDocument::getRuleName)
              .collect(Collectors.toSet());

      GetSamplingTargetsResponse response =
          client.getSamplingTargets(GetSamplingTargetsRequest.create(statistics));
      Map<String, SamplingTargetDocument> targets =
          response.getDocuments().stream()
              .collect(Collectors.toMap(SamplingTargetDocument::getRuleName, Function.identity()));
      sampler =
          xrayRulesSampler = xrayRulesSampler.withTargets(targets, requestedTargetRuleNames, now);
    } catch (Throwable t) {
      // Might be a transient API failure, try again after a default interval.
      fetchTargetsFuture =
          executor.schedule(
              this::fetchTargets, DEFAULT_TARGET_INTERVAL_NANOS, TimeUnit.NANOSECONDS);
      return;
    }

    long nextTargetFetchIntervalNanos =
        xrayRulesSampler.nextTargetFetchTimeNanos() - clock.nanoTime();
    fetchTargetsFuture =
        executor.schedule(this::fetchTargets, nextTargetFetchIntervalNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  @SuppressWarnings("Interruption")
  public void close() {
    ScheduledFuture<?> pollFuture = this.pollFuture;
    if (pollFuture != null) {
      pollFuture.cancel(true);
    }
    executor.shutdownNow();
    // No flushing behavior so no need to wait for the shutdown.
  }

  private static String generateClientId() {
    Random rand = new Random();
    char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    char[] clientIdChars = new char[24];
    for (int i = 0; i < clientIdChars.length; i++) {
      clientIdChars[i] = hex[rand.nextInt(hex.length)];
    }
    return new String(clientIdChars);
  }

  // Visible for testing
  XraySamplerClient getClient() {
    return client;
  }

  // Visible for testing
  Resource getResource() {
    return resource;
  }
}
