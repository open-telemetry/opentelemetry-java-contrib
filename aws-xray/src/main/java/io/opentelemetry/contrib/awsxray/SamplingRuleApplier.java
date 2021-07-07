/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

final class SamplingRuleApplier {

  private static final Map<String, String> XRAY_CLOUD_PLATFORM;

  static {
    Map<String, String> xrayCloudPlatform = new HashMap<>();
    xrayCloudPlatform.put(ResourceAttributes.CloudPlatformValues.AWS_EC2, "AWS::EC2::Instance");
    xrayCloudPlatform.put(ResourceAttributes.CloudPlatformValues.AWS_ECS, "AWS::ECS::Container");
    xrayCloudPlatform.put(ResourceAttributes.CloudPlatformValues.AWS_EKS, "AWS::EKS::Container");
    xrayCloudPlatform.put(
        ResourceAttributes.CloudPlatformValues.AWS_ELASTIC_BEANSTALK,
        "AWS::ElasticBeanstalk::Environment");
    xrayCloudPlatform.put(
        ResourceAttributes.CloudPlatformValues.AWS_LAMBDA, "AWS::Lambda::Function");
    XRAY_CLOUD_PLATFORM = Collections.unmodifiableMap(xrayCloudPlatform);
  }

  private final String clientId;
  private final String ruleName;
  private final Clock clock;
  private final Sampler reservoirSampler;
  private final long reservoirEndTimeNanos;
  private final Sampler fixedRateSampler;
  private final boolean borrowing;

  private final Map<String, Matcher> attributeMatchers;
  private final Matcher urlPathMatcher;
  private final Matcher serviceNameMatcher;
  private final Matcher httpMethodMatcher;
  private final Matcher hostMatcher;
  private final Matcher serviceTypeMatcher;
  private final Matcher resourceArnMatcher;

  private final Statistics statistics;

  private final long nextSnapshotTimeNanos;

  SamplingRuleApplier(String clientId, GetSamplingRulesResponse.SamplingRule rule, Clock clock) {
    this.clientId = clientId;
    this.clock = clock;
    ruleName = rule.getRuleName();

    // We don't have a SamplingTarget so are ready to report a snapshot right away.
    nextSnapshotTimeNanos = clock.nanoTime();

    // We either have no reservoir sampling or borrow until we get a quota so have no end time.
    reservoirEndTimeNanos = Long.MAX_VALUE;

    if (rule.getReservoirSize() > 0) {
      // Until calling GetSamplingTargets, the default is to borrow 1/s if reservoir size is
      // positive.
      reservoirSampler = createRateLimited(1);
      borrowing = true;
    } else {
      // No reservoir sampling, we will always use the fixed rate.
      reservoirSampler = Sampler.alwaysOff();
      borrowing = false;
    }
    fixedRateSampler = createFixedRate(rule.getFixedRate());

    if (rule.getAttributes().isEmpty()) {
      attributeMatchers = Collections.emptyMap();
    } else {
      attributeMatchers =
          rule.getAttributes().entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> toMatcher(e.getValue())));
    }

    urlPathMatcher = toMatcher(rule.getUrlPath());
    serviceNameMatcher = toMatcher(rule.getServiceName());
    httpMethodMatcher = toMatcher(rule.getHttpMethod());
    hostMatcher = toMatcher(rule.getHost());
    serviceTypeMatcher = toMatcher(rule.getServiceType());
    resourceArnMatcher = toMatcher(rule.getResourceArn());

    statistics = new Statistics();
  }

  private SamplingRuleApplier(
      String clientId,
      String ruleName,
      Clock clock,
      Sampler reservoirSampler,
      long reservoirEndTimeNanos,
      Sampler fixedRateSampler,
      boolean borrowing,
      Map<String, Matcher> attributeMatchers,
      Matcher urlPathMatcher,
      Matcher serviceNameMatcher,
      Matcher httpMethodMatcher,
      Matcher hostMatcher,
      Matcher serviceTypeMatcher,
      Matcher resourceArnMatcher,
      Statistics statistics,
      long nextSnapshotTimeNanos) {
    this.clientId = clientId;
    this.ruleName = ruleName;
    this.clock = clock;
    this.reservoirSampler = reservoirSampler;
    this.reservoirEndTimeNanos = reservoirEndTimeNanos;
    this.fixedRateSampler = fixedRateSampler;
    this.borrowing = borrowing;
    this.attributeMatchers = attributeMatchers;
    this.urlPathMatcher = urlPathMatcher;
    this.serviceNameMatcher = serviceNameMatcher;
    this.httpMethodMatcher = httpMethodMatcher;
    this.hostMatcher = hostMatcher;
    this.serviceTypeMatcher = serviceTypeMatcher;
    this.resourceArnMatcher = resourceArnMatcher;
    this.statistics = statistics;
    this.nextSnapshotTimeNanos = nextSnapshotTimeNanos;
  }

  boolean matches(String name, Attributes attributes, Resource resource) {
    int matchedAttributes = 0;
    String httpTarget = null;
    String httpMethod = null;
    String host = null;

    for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
      if (entry.getKey().equals(SemanticAttributes.HTTP_TARGET)) {
        httpTarget = (String) entry.getValue();
      } else if (entry.getKey().equals(SemanticAttributes.HTTP_METHOD)) {
        httpMethod = (String) entry.getValue();
      } else if (entry.getKey().equals(SemanticAttributes.HTTP_HOST)) {
        host = (String) entry.getValue();
      }

      Matcher matcher = attributeMatchers.get(entry.getKey().getKey());
      if (matcher == null) {
        continue;
      }
      if (matcher.matches(entry.getValue().toString())) {
        matchedAttributes++;
      } else {
        return false;
      }
    }
    // All attributes in the matched attributes must have been present in the span to be a match.
    if (matchedAttributes != attributeMatchers.size()) {
      return false;
    }

    return urlPathMatcher.matches(httpTarget)
        && serviceNameMatcher.matches(name)
        && httpMethodMatcher.matches(httpMethod)
        && hostMatcher.matches(host)
        && serviceTypeMatcher.matches(getServiceType(resource))
        && resourceArnMatcher.matches(getArn(attributes, resource));
  }

  SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    // Incrementing requests first ensures sample / borrow rate are positive.
    statistics.requests.increment();
    SamplingResult result =
        reservoirSampler.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);
    if (result.getDecision() != SamplingDecision.DROP) {
      // We use the result from the reservoir sampler if it worked.
      if (borrowing) {
        statistics.borrowed.increment();
      }
      statistics.sampled.increment();
      return result;
    }
    result =
        fixedRateSampler.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);
    if (result.getDecision() != SamplingDecision.DROP) {
      statistics.sampled.increment();
    }
    return result;
  }

  @Nullable
  SamplingStatisticsDocument snapshot(Date now) {
    if (clock.nanoTime() < nextSnapshotTimeNanos) {
      return null;
    }
    return SamplingStatisticsDocument.newBuilder()
        .setClientId(clientId)
        .setRuleName(ruleName)
        .setTimestamp(now)
        // Resetting requests first ensures that sample / borrow rate are positive after the reset.
        // Snapshotting is not concurrent so this ensures they are always positive.
        .setRequestCount(statistics.requests.sumThenReset())
        .setSampledCount(statistics.sampled.sumThenReset())
        .setBorrowCount(statistics.borrowed.sumThenReset())
        .build();
  }

  long getNextSnapshotTimeNanos() {
    return nextSnapshotTimeNanos;
  }

  SamplingRuleApplier withTarget(SamplingTargetDocument target, Date now) {
    Sampler newFixedRateSampler = createFixedRate(target.getFixedRate());
    Sampler newReservoirSampler = Sampler.alwaysOff();
    long newReservoirEndTimeNanos = System.nanoTime();
    // Not well documented but a quota should always come with a TTL
    if (target.getReservoirQuota() != null && target.getReservoirQuotaTtl() != null) {
      newReservoirSampler = createRateLimited(target.getReservoirQuota());
      newReservoirEndTimeNanos =
          clock.nanoTime()
              + Duration.between(now.toInstant(), target.getReservoirQuotaTtl().toInstant())
                  .toNanos();
    }
    long intervalNanos =
        target.getIntervalSecs() != null
            ? TimeUnit.SECONDS.toNanos(target.getIntervalSecs())
            : AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS;
    long newNextSnapshotTimeNanos = System.nanoTime() + intervalNanos;

    return new SamplingRuleApplier(
        clientId,
        ruleName,
        clock,
        newReservoirSampler,
        newReservoirEndTimeNanos,
        newFixedRateSampler,
        /* borrowing= */ false,
        attributeMatchers,
        urlPathMatcher,
        serviceNameMatcher,
        httpMethodMatcher,
        hostMatcher,
        serviceTypeMatcher,
        resourceArnMatcher,
        statistics,
        newNextSnapshotTimeNanos);
  }

  SamplingRuleApplier withNextSnapshotTimeNanos(long newNextSnapshotTimeNanos) {
    return new SamplingRuleApplier(
        clientId,
        ruleName,
        clock,
        reservoirSampler,
        reservoirEndTimeNanos,
        fixedRateSampler,
        borrowing,
        attributeMatchers,
        urlPathMatcher,
        serviceNameMatcher,
        httpMethodMatcher,
        hostMatcher,
        serviceTypeMatcher,
        resourceArnMatcher,
        statistics,
        newNextSnapshotTimeNanos);
  }

  String getRuleName() {
    return ruleName;
  }

  @Nullable
  private static String getArn(Attributes attributes, Resource resource) {
    String arn = resource.getAttributes().get(ResourceAttributes.AWS_ECS_CONTAINER_ARN);
    if (arn != null) {
      return arn;
    }
    String cloudPlatform = resource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM);
    if (ResourceAttributes.CloudPlatformValues.AWS_LAMBDA.equals(cloudPlatform)) {
      return getLambdaArn(attributes, resource);
    }
    return null;
  }

  @Nullable
  private static String getLambdaArn(Attributes attributes, Resource resource) {
    String arn = resource.getAttributes().get(ResourceAttributes.FAAS_ID);
    if (arn != null) {
      return arn;
    }
    return attributes.get(ResourceAttributes.FAAS_ID);
  }

  @Nullable
  private static String getServiceType(Resource resource) {
    String cloudPlatform = resource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM);
    if (cloudPlatform == null) {
      return null;
    }
    return XRAY_CLOUD_PLATFORM.get(cloudPlatform);
  }

  private static Matcher toMatcher(String globPattern) {
    if (globPattern.equals("*")) {
      return TrueMatcher.INSTANCE;
    }

    for (int i = 0; i < globPattern.length(); i++) {
      char c = globPattern.charAt(i);
      if (c == '*' || c == '?') {
        return new PatternMatcher(toRegexPattern(globPattern));
      }
    }

    return new StringMatcher(globPattern);
  }

  private static Pattern toRegexPattern(String globPattern) {
    int tokenStart = -1;
    StringBuilder patternBuilder = new StringBuilder();
    for (int i = 0; i < globPattern.length(); i++) {
      char c = globPattern.charAt(i);
      if (c == '*' || c == '?') {
        if (tokenStart != -1) {
          patternBuilder.append(Pattern.quote(globPattern.substring(tokenStart, i)));
          tokenStart = -1;
        }
        if (c == '*') {
          patternBuilder.append(".*");
        } else {
          // c == '?'
          patternBuilder.append(".");
        }
      } else {
        if (tokenStart == -1) {
          tokenStart = i;
        }
      }
    }
    if (tokenStart != -1) {
      patternBuilder.append(Pattern.quote(globPattern.substring(tokenStart)));
    }
    return Pattern.compile(patternBuilder.toString());
  }

  private interface Matcher {
    boolean matches(@Nullable String s);
  }

  private enum TrueMatcher implements Matcher {
    INSTANCE;

    @Override
    public boolean matches(@Nullable String s) {
      return true;
    }

    @Override
    public String toString() {
      return "TrueMatcher";
    }
  }

  private static class StringMatcher implements Matcher {

    private final String target;

    StringMatcher(String target) {
      this.target = target;
    }

    @Override
    public boolean matches(@Nullable String s) {
      if (s == null) {
        return false;
      }
      return target.equalsIgnoreCase(s);
    }

    @Override
    public String toString() {
      return target;
    }
  }

  private static class PatternMatcher implements Matcher {
    private final Pattern pattern;

    PatternMatcher(Pattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public boolean matches(@Nullable String s) {
      if (s == null) {
        return false;
      }
      return pattern.matcher(s).matches();
    }

    @Override
    public String toString() {
      return pattern.toString();
    }
  }

  private Sampler createRateLimited(int numPerSecond) {
    return Sampler.parentBased(new RateLimitingSampler(numPerSecond, clock));
  }

  private Sampler createFixedRate(double rate) {
    return Sampler.parentBased(Sampler.traceIdRatioBased(rate));
  }

  // We keep track of sampling requests and decisions to report to X-Ray to allow it to allocate
  // quota from the central reservoir. We do not lock around updates because sampling is called on
  // the hot, highly-contended path and locking would have significant overhead. The actual possible
  // error should not be off to significantly affect quotas in practice.
  private static class Statistics {
    final LongAdder requests = new LongAdder();
    final LongAdder sampled = new LongAdder();
    final LongAdder borrowed = new LongAdder();
  }
}
