/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toList;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class XrayRulesSampler implements Sampler {

  private static final Logger logger = Logger.getLogger(XrayRulesSampler.class.getName());

  public static final AttributeKey<String> AWS_XRAY_SAMPLING_RULE =
      AttributeKey.stringKey("aws.xray.sampling_rule");

  // Used for generating operation
  private static final String UNKNOWN_OPERATION = "UnknownOperation";
  private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
  private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
  private static final AttributeKey<String> HTTP_REQUEST_METHOD =
      AttributeKey.stringKey("http.request.method");
  private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");

  private final String clientId;
  private final Resource resource;
  private final Clock clock;
  private final Sampler fallbackSampler;
  private final SamplingRuleApplier[] ruleAppliers;
  private final Map<String, String> ruleToHashMap;
  private final Map<String, String> hashToRuleMap;

  private final boolean adaptiveSamplingRuleExists;
  private final Cache<String, AwsXrayAdaptiveSamplingConfig.UsageType> traceUsageCache;

  @Nullable private AwsXrayAdaptiveSamplingConfig adaptiveSamplingConfig;
  @Nullable private RateLimiter anomalyCaptureRateLimiter;

  XrayRulesSampler(
      String clientId,
      Resource resource,
      Clock clock,
      Sampler fallbackSampler,
      List<GetSamplingRulesResponse.SamplingRule> rules,
      @Nullable AwsXrayAdaptiveSamplingConfig adaptiveSamplingConfig) {
    this(
        clientId,
        resource,
        clock,
        fallbackSampler,
        rules.stream()
            // Lower priority value takes precedence so normal ascending sort.
            .sorted(Comparator.comparingInt(GetSamplingRulesResponse.SamplingRule::getPriority))
            .map(
                rule ->
                    new SamplingRuleApplier(
                        clientId, rule, resource.getAttribute(SERVICE_NAME), clock))
            .toArray(SamplingRuleApplier[]::new),
        createRuleHashMaps(rules),
        rules.stream().anyMatch(r -> r.getSamplingRateBoost() != null),
        adaptiveSamplingConfig,
        Caffeine.newBuilder()
            .maximumSize(100_000)
            .ticker(clock::nanoTime)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build());
  }

  private XrayRulesSampler(
      String clientId,
      Resource resource,
      Clock clock,
      Sampler fallbackSampler,
      SamplingRuleApplier[] ruleAppliers,
      Map<String, String> ruleToHashMap,
      boolean adaptiveSamplingRuleExists,
      @Nullable AwsXrayAdaptiveSamplingConfig adaptiveSamplingConfig,
      Cache<String, AwsXrayAdaptiveSamplingConfig.UsageType> traceUsageCache) {
    this.clientId = clientId;
    this.resource = resource;
    this.clock = clock;
    this.fallbackSampler = fallbackSampler;
    this.ruleAppliers = ruleAppliers;
    this.ruleToHashMap = ruleToHashMap;
    this.hashToRuleMap = new HashMap<>();
    for (Map.Entry<String, String> entry : ruleToHashMap.entrySet()) {
      this.hashToRuleMap.put(entry.getValue(), entry.getKey());
    }
    this.adaptiveSamplingRuleExists = adaptiveSamplingRuleExists;
    this.adaptiveSamplingConfig = adaptiveSamplingConfig;
    this.traceUsageCache = traceUsageCache;

    // Initialize anomaly capture rate limiter
    if (this.adaptiveSamplingConfig != null
        && this.adaptiveSamplingConfig.getAnomalyCaptureLimit() == null) {
      this.anomalyCaptureRateLimiter = new RateLimiter(1, 1, clock);
    } else if (adaptiveSamplingConfig != null
        && adaptiveSamplingConfig.getAnomalyCaptureLimit() != null) {
      int anomalyTracesPerSecond =
          adaptiveSamplingConfig.getAnomalyCaptureLimit().getAnomalyTracesPerSecond();
      this.anomalyCaptureRateLimiter =
          new RateLimiter(anomalyTracesPerSecond, anomalyTracesPerSecond, clock);
    }
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    String upstreamMatchedRule =
        parentSpanContext
            .getTraceState()
            .get(AwsSamplingResult.AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY);
    if (upstreamMatchedRule == null) {
      Baggage b = Baggage.fromContext(parentContext);
      upstreamMatchedRule =
          b != null
              ? b.getEntryValue(AwsSamplingResult.AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY)
              : null;
    }
    for (SamplingRuleApplier applier : ruleAppliers) {
      if (applier.matches(attributes, resource)) {
        SamplingResult result =
            applier.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);

        // If the trace state has a sampling rule reference, propagate it
        // Otherwise, encode and propagate the matched sampling rule using AwsSamplingResult
        String ruleToPropagate;
        if (upstreamMatchedRule != null) {
          ruleToPropagate = hashToRuleMap.getOrDefault(upstreamMatchedRule, null);
        } else if (parentSpanContext.isValid()) {
          ruleToPropagate = null;
        } else {
          ruleToPropagate = applier.getRuleName();
        }
        String hashedRule = ruleToHashMap.getOrDefault(ruleToPropagate, upstreamMatchedRule);

        return AwsSamplingResult.create(
            result.getDecision(),
            result.getAttributes().toBuilder()
                .put(
                    AWS_XRAY_SAMPLING_RULE.getKey(),
                    ruleToPropagate != null ? ruleToPropagate : "UNKNOWN")
                .build(),
            hashedRule);
      }
    }

    // In practice, X-Ray always returns a Default rule that matches all requests so it is a bug in
    // our code or X-Ray to reach here, fallback just in case.
    logger.log(
        FINE,
        "No sampling rule matched the request. "
            + "This is a bug in either the OpenTelemetry SDK or X-Ray.");
    return fallbackSampler.shouldSample(
        parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "XrayRulesSampler{" + Arrays.toString(ruleAppliers) + "}";
  }

  void setAdaptiveSamplingConfig(AwsXrayAdaptiveSamplingConfig config) {
    if (this.adaptiveSamplingConfig != null) {
      throw new IllegalStateException("Programming bug - Adaptive sampling config is already set");
    } else if (config != null && this.adaptiveSamplingConfig == null) {
      this.adaptiveSamplingConfig = config;

      // Initialize anomaly capture rate limiter if error capture limit is configured
      if (config.getAnomalyCaptureLimit() != null) {
        int anomalyTracesPerSecond = config.getAnomalyCaptureLimit().getAnomalyTracesPerSecond();
        this.anomalyCaptureRateLimiter =
            new RateLimiter(anomalyTracesPerSecond, anomalyTracesPerSecond, clock);
      } else {
        this.anomalyCaptureRateLimiter = new RateLimiter(1, 1, clock);
      }
    }
  }

  void adaptSampling(ReadableSpan span, SpanData spanData, Consumer<ReadableSpan> spanBatcher) {
    if (!adaptiveSamplingRuleExists && this.adaptiveSamplingConfig == null) {
      return;
    }

    AnomalyDetectionResult result = isAnomaly(span, spanData);
    boolean shouldBoostSampling = result.shouldBoostSampling();
    boolean shouldCaptureAnomalySpan = result.shouldCaptureAnomalySpan();

    String traceId = spanData.getTraceId();
    AwsXrayAdaptiveSamplingConfig.UsageType existingUsage = traceUsageCache.getIfPresent(traceId);
    boolean isNewTrace = existingUsage == null;

    // Anomaly Capture
    boolean isSpanCaptured = false;
    if (AwsXrayAdaptiveSamplingConfig.UsageType.isUsedForAnomalyTraceCapture(existingUsage)
        || (shouldCaptureAnomalySpan
            && !span.getSpanContext().isSampled()
            && anomalyCaptureRateLimiter != null
            && anomalyCaptureRateLimiter.trySpend(1))) {
      spanBatcher.accept(span);
      isSpanCaptured = true;
    }

    // Sampling Boost
    boolean isCountedAsAnomalyForBoost = false;
    if (shouldBoostSampling || isNewTrace) {
      String traceStateValue =
          span.getSpanContext()
              .getTraceState()
              .get(AwsSamplingResult.AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY);
      String upstreamRuleName =
          traceStateValue != null
              ? hashToRuleMap.getOrDefault(traceStateValue, traceStateValue)
              : traceStateValue;
      SamplingRuleApplier ruleToReportTo = null;
      SamplingRuleApplier matchedRule = null;
      for (SamplingRuleApplier applier : ruleAppliers) {
        // Rule propagated from when sampling decision was made, otherwise the matched rule
        if (applier.getRuleName().equals(upstreamRuleName)) {
          ruleToReportTo = applier;
          break;
        }
        if (applier.matches(spanData.getAttributes(), resource)) {
          matchedRule = applier;
        }
      }
      if (ruleToReportTo == null) {
        if (matchedRule == null) {
          logger.log(
              FINE,
              "No sampling rule matched the request. This is a bug in either the OpenTelemetry SDK or X-Ray.");
        } else if (!span.getParentSpanContext().isValid()) {
          // Span is not from an upstream service, so we should boost the matched rule
          ruleToReportTo = matchedRule;
        }
      }

      if (shouldBoostSampling
          && ruleToReportTo != null
          && ruleToReportTo.hasBoost()
          && !AwsXrayAdaptiveSamplingConfig.UsageType.isUsedForBoost(existingUsage)) {
        ruleToReportTo.countAnomalyTrace(span);
        isCountedAsAnomalyForBoost = true;
      }
      if (isNewTrace && ruleToReportTo != null && ruleToReportTo.hasBoost()) {
        ruleToReportTo.countTrace();
      }
    }

    updateTraceUsageCache(traceId, isSpanCaptured, isCountedAsAnomalyForBoost);
  }

  List<SamplingRuleApplier.SamplingRuleStatisticsSnapshot> snapshot(Date now) {
    return Arrays.stream(ruleAppliers)
        .map(rule -> rule.snapshot(now))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  long nextTargetFetchTimeNanos() {
    return Arrays.stream(ruleAppliers)
        .mapToLong(SamplingRuleApplier::getNextSnapshotTimeNanos)
        .min()
        // There is always at least one rule in practice so this should never be exercised.
        .orElseGet(() -> clock.nanoTime() + AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS);
  }

  XrayRulesSampler withTargets(
      Map<String, SamplingTargetDocument> ruleTargets,
      Set<String> requestedTargetRuleNames,
      Date now) {
    long currentNanoTime = clock.nanoTime();
    long defaultNextSnapshotTimeNanos =
        currentNanoTime + AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS;
    SamplingRuleApplier[] newAppliers =
        Arrays.stream(ruleAppliers)
            .map(
                rule -> {
                  SamplingTargetDocument target = ruleTargets.get(rule.getRuleName());
                  if (target != null) {
                    return rule.withTarget(target, now, currentNanoTime);
                  }
                  if (requestedTargetRuleNames.contains(rule.getRuleName())) {
                    // In practice X-Ray should return a target for any rule we requested but
                    // do a defensive check here in case. If we requested a target but got nothing
                    // back assume the default interval.
                    return rule.withNextSnapshotTimeNanos(defaultNextSnapshotTimeNanos);
                  }
                  // Target not requested, will be updated in a future target fetch.
                  return rule;
                })
            .toArray(SamplingRuleApplier[]::new);
    return new XrayRulesSampler(
        clientId,
        resource,
        clock,
        fallbackSampler,
        newAppliers,
        ruleToHashMap,
        adaptiveSamplingRuleExists,
        adaptiveSamplingConfig,
        traceUsageCache);
  }

  private AnomalyDetectionResult isAnomaly(ReadableSpan span, SpanData spanData) {
    boolean shouldBoostSampling = false;
    boolean shouldCaptureAnomalySpan = false;
    Long statusCode = spanData.getAttributes().get(HTTP_RESPONSE_STATUS_CODE);

    List<AwsXrayAdaptiveSamplingConfig.AnomalyConditions> anomalyConditions =
        adaptiveSamplingConfig != null ? adaptiveSamplingConfig.getAnomalyConditions() : null;
    // Empty list -> no conditions will apply and we will not do anything
    if (anomalyConditions != null) {
      String operation = spanData.getAttributes().get(AwsAttributeKeys.AWS_LOCAL_OPERATION);
      if (operation == null) {
        operation = generateIngressOperation(spanData);
      }
      for (AwsXrayAdaptiveSamplingConfig.AnomalyConditions condition : anomalyConditions) {
        // Skip condition if it would only re-apply action already being taken
        if ((shouldBoostSampling
                && AwsXrayAdaptiveSamplingConfig.UsageType.SAMPLING_BOOST.equals(
                    condition.getUsage()))
            || (shouldCaptureAnomalySpan
                && AwsXrayAdaptiveSamplingConfig.UsageType.ANOMALY_TRACE_CAPTURE.equals(
                    condition.getUsage()))) {
          continue;
        }
        // Check if the operation matches any in the list or if operations list is null (match all)
        List<String> operations = condition.getOperations();
        if (!(operations == null || operations.isEmpty() || operations.contains(operation))) {
          continue;
        }
        // Check if any anomalyConditions detect an anomaly either through error code or latency
        boolean isAnomaly = false;

        String errorCodeRegex = condition.getErrorCodeRegex();
        if (statusCode != null && errorCodeRegex != null) {
          isAnomaly = statusCode.toString().matches(errorCodeRegex);
        }

        Long highLatencyMs = condition.getHighLatencyMs();
        if (highLatencyMs != null) {
          isAnomaly =
              (errorCodeRegex == null || isAnomaly)
                  && (span.getLatencyNanos() / 1_000_000.0) >= highLatencyMs;
        }

        if (isAnomaly) {
          AwsXrayAdaptiveSamplingConfig.UsageType usage = condition.getUsage();
          if (usage != null) {
            switch (usage) {
              case BOTH:
                shouldBoostSampling = true;
                shouldCaptureAnomalySpan = true;
                break;
              case SAMPLING_BOOST:
                shouldBoostSampling = true;
                break;
              case ANOMALY_TRACE_CAPTURE:
                shouldCaptureAnomalySpan = true;
                break;
              default: // do nothing
            }
          } else {
            shouldBoostSampling = true;
            shouldCaptureAnomalySpan = true;
          }
        }
        if (shouldBoostSampling && shouldCaptureAnomalySpan) {
          break;
        }
      }
    } else if ((statusCode != null && statusCode > 499)
        || (statusCode == null
            && spanData.getStatus() != null
            && StatusCode.ERROR.equals(spanData.getStatus().getStatusCode()))) {
      shouldBoostSampling = true;
      shouldCaptureAnomalySpan = true;
    }

    return new AnomalyDetectionResult(shouldBoostSampling, shouldCaptureAnomalySpan);
  }

  static boolean isKeyPresent(SpanData span, AttributeKey<?> key) {
    return span.getAttributes().get(key) != null;
  }

  private static String generateIngressOperation(SpanData span) {
    String operation = UNKNOWN_OPERATION;
    if (isKeyPresent(span, URL_PATH) || isKeyPresent(span, HTTP_TARGET)) {
      String httpTarget =
          isKeyPresent(span, URL_PATH)
              ? span.getAttributes().get(URL_PATH)
              : span.getAttributes().get(HTTP_TARGET);
      // get the first part from API path string as operation value
      // the more levels/parts we get from API path the higher chance for getting high cardinality
      // data
      if (httpTarget != null) {
        operation = extractApiPathValue(httpTarget);
        if (isKeyPresent(span, HTTP_REQUEST_METHOD) || isKeyPresent(span, HTTP_METHOD)) {
          String httpMethod =
              isKeyPresent(span, HTTP_REQUEST_METHOD)
                  ? span.getAttributes().get(HTTP_REQUEST_METHOD)
                  : span.getAttributes().get(HTTP_METHOD);
          if (httpMethod != null) {
            operation = httpMethod + " " + operation;
          }
        }
      }
    }
    return operation;
  }

  private static String extractApiPathValue(String httpTarget) {
    if (httpTarget == null || httpTarget.isEmpty()) {
      return "/";
    }
    String[] paths = httpTarget.split("/");
    if (paths.length > 1) {
      return "/" + paths[1];
    }
    return "/";
  }

  private void updateTraceUsageCache(
      String traceId, boolean isSpanCaptured, boolean isCountedAsAnomalyForBoost) {
    AwsXrayAdaptiveSamplingConfig.UsageType existingUsage = traceUsageCache.getIfPresent(traceId);

    // Any interaction with a cache entry will reset the expiration timer of that entry
    if (isSpanCaptured && isCountedAsAnomalyForBoost) {
      this.traceUsageCache.put(traceId, AwsXrayAdaptiveSamplingConfig.UsageType.BOTH);
    } else if (isSpanCaptured) {
      if (AwsXrayAdaptiveSamplingConfig.UsageType.isUsedForBoost(existingUsage)) {
        this.traceUsageCache.put(traceId, AwsXrayAdaptiveSamplingConfig.UsageType.BOTH);
      } else {
        this.traceUsageCache.put(
            traceId, AwsXrayAdaptiveSamplingConfig.UsageType.ANOMALY_TRACE_CAPTURE);
      }
    } else if (isCountedAsAnomalyForBoost) {
      if (AwsXrayAdaptiveSamplingConfig.UsageType.isUsedForAnomalyTraceCapture(existingUsage)) {
        this.traceUsageCache.put(traceId, AwsXrayAdaptiveSamplingConfig.UsageType.BOTH);
      } else {
        this.traceUsageCache.put(traceId, AwsXrayAdaptiveSamplingConfig.UsageType.SAMPLING_BOOST);
      }
    } else if (existingUsage != null) {
      this.traceUsageCache.put(traceId, existingUsage);
    } else {
      this.traceUsageCache.put(traceId, AwsXrayAdaptiveSamplingConfig.UsageType.NEITHER);
    }
  }

  private static Map<String, String> createRuleHashMaps(
      List<GetSamplingRulesResponse.SamplingRule> rules) {
    Map<String, String> ruleToHashMap = new HashMap<>();
    for (GetSamplingRulesResponse.SamplingRule rule : rules) {
      String ruleName = rule.getRuleName();
      if (ruleName != null) {
        ruleToHashMap.put(ruleName, hashRuleName(ruleName));
      }
    }
    return ruleToHashMap;
  }

  static String hashRuleName(String ruleName) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(ruleName.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < Math.min(hash.length, 8); i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return ruleName;
    }
  }

  // For testing
  Cache<String, AwsXrayAdaptiveSamplingConfig.UsageType> getTraceUsageCache() {
    traceUsageCache.cleanUp();
    return traceUsageCache;
  }

  private static class AnomalyDetectionResult {
    private final boolean shouldBoostSampling;
    private final boolean shouldCaptureAnomalySpan;

    AnomalyDetectionResult(boolean shouldBoostSampling, boolean shouldCaptureAnomalySpan) {
      this.shouldBoostSampling = shouldBoostSampling;
      this.shouldCaptureAnomalySpan = shouldCaptureAnomalySpan;
    }

    boolean shouldBoostSampling() {
      return shouldBoostSampling;
    }

    boolean shouldCaptureAnomalySpan() {
      return shouldCaptureAnomalySpan;
    }
  }
}
