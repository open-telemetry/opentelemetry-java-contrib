/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingRulesResponse.SamplingRuleRecord;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingBoostStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Remote sampler that gets sampling configuration from AWS X-Ray. */
public final class AwsXrayRemoteSampler implements Sampler, Closeable {

  static final long DEFAULT_TARGET_INTERVAL_NANOS = SECONDS.toNanos(10);

  private static final Logger logger = Logger.getLogger(AwsXrayRemoteSampler.class.getName());

  // Default batch size to be same as OTel BSP default
  private static final int maxExportBatchSize = 512;

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

  @Nullable private AwsXrayAdaptiveSamplingConfig adaptiveSamplingConfig;
  @Nullable private BatchSpanProcessor bsp;

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

  public void setAdaptiveSamplingConfig(AwsXrayAdaptiveSamplingConfig config) {
    if (this.adaptiveSamplingConfig != null) {
      throw new IllegalStateException("Programming bug - Adaptive sampling config is already set");
    } else if (config != null && this.adaptiveSamplingConfig == null) {
      // Save here and also pass to XrayRulesSampler directly as it already exists
      this.adaptiveSamplingConfig = config;
      if (sampler instanceof XrayRulesSampler) {
        ((XrayRulesSampler) sampler).setAdaptiveSamplingConfig(config);
      }
    }
  }

  public void setSpanExporter(SpanExporter spanExporter) {
    if (this.bsp != null) {
      throw new IllegalStateException("Programming bug - BatchSpanProcessor is already set");
    } else if (spanExporter != null && this.bsp == null) {
      this.bsp =
          BatchSpanProcessor.builder(spanExporter)
              .setExportUnsampledSpans(true) // Required to capture the unsampled anomaly spans
              .setMaxExportBatchSize(maxExportBatchSize)
              .build();
    }
  }

  public void adaptSampling(ReadableSpan span, SpanData spanData) {
    if (this.bsp == null) {
      throw new IllegalStateException(
          "Programming bug - BatchSpanProcessor is null while trying to adapt sampling");
    }
    if (sampler instanceof XrayRulesSampler) {
      ((XrayRulesSampler) sampler).adaptSampling(span, spanData, this.bsp::onEnd);
    }
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
                    .collect(toList()),
                adaptiveSamplingConfig);
        previousRulesResponse = response;
        ScheduledFuture<?> existingFetchTargetsFuture = fetchTargetsFuture;
        if (existingFetchTargetsFuture != null) {
          existingFetchTargetsFuture.cancel(false);
        }
        fetchTargetsFuture =
            executor.schedule(this::fetchTargets, DEFAULT_TARGET_INTERVAL_NANOS, NANOSECONDS);
      }
    } catch (Throwable t) {
      logger.log(FINE, "Failed to update sampler", t);
    }
    scheduleSamplerUpdate();
  }

  private void scheduleSamplerUpdate() {
    long delay = pollingIntervalNanos + jitterNanos.next();
    pollFuture = executor.schedule(this::getAndUpdateSampler, delay, NANOSECONDS);
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
    return Duration.ofNanos(pollFuture.getDelay(NANOSECONDS));
  }

  private void fetchTargets() {
    if (!(sampler instanceof XrayRulesSampler)) {
      throw new IllegalStateException("Programming bug.");
    }

    XrayRulesSampler xrayRulesSampler = (XrayRulesSampler) sampler;
    try {
      Date now = Date.from(Instant.ofEpochSecond(0, clock.now()));
      List<SamplingRuleApplier.SamplingRuleStatisticsSnapshot> statisticsSnapshot =
          xrayRulesSampler.snapshot(now);
      List<SamplingStatisticsDocument> statistics = new ArrayList<SamplingStatisticsDocument>();
      List<SamplingBoostStatisticsDocument> boostStatistics =
          new ArrayList<SamplingBoostStatisticsDocument>();
      statisticsSnapshot.stream()
          .forEach(
              snapshot -> {
                if (snapshot.getStatisticsDocument() != null) {
                  statistics.add(snapshot.getStatisticsDocument());
                }
                if (snapshot.getBoostStatisticsDocument() != null
                    && snapshot.getBoostStatisticsDocument().getTotalCount() > 0) {
                  boostStatistics.add(snapshot.getBoostStatisticsDocument());
                }
              });
      Set<String> requestedTargetRuleNames =
          statistics.stream().map(SamplingStatisticsDocument::getRuleName).collect(toSet());

      GetSamplingTargetsRequest req = GetSamplingTargetsRequest.create(statistics, boostStatistics);
      GetSamplingTargetsResponse response = client.getSamplingTargets(req);
      Map<String, SamplingTargetDocument> targets =
          response.getDocuments().stream()
              .collect(toMap(SamplingTargetDocument::getRuleName, identity()));
      sampler =
          xrayRulesSampler = xrayRulesSampler.withTargets(targets, requestedTargetRuleNames, now);
    } catch (Throwable t) {
      // Might be a transient API failure, try again after a default interval.
      fetchTargetsFuture =
          executor.schedule(this::fetchTargets, DEFAULT_TARGET_INTERVAL_NANOS, NANOSECONDS);
      return;
    }

    long nextTargetFetchIntervalNanos =
        xrayRulesSampler.nextTargetFetchTimeNanos() - clock.nanoTime();
    fetchTargetsFuture =
        executor.schedule(this::fetchTargets, nextTargetFetchIntervalNanos, NANOSECONDS);
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
