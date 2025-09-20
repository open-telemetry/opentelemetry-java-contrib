/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingBoostStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsRequest.SamplingStatisticsDocument;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingBoost;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

final class SamplingRuleApplier {

  // copied from AwsIncubatingAttributes
  private static final AttributeKey<String> AWS_ECS_CONTAINER_ARN =
      stringKey("aws.ecs.container.arn");
  // copied from CloudIncubatingAttributes
  private static final AttributeKey<String> CLOUD_PLATFORM = stringKey("cloud.platform");
  private static final AttributeKey<String> CLOUD_RESOURCE_ID = stringKey("cloud.resource_id");
  // copied from CloudIncubatingAttributes.CloudPlatformIncubatingValues
  public static final String AWS_EC2 = "aws_ec2";
  public static final String AWS_ECS = "aws_ecs";
  public static final String AWS_EKS = "aws_eks";
  public static final String AWS_LAMBDA = "aws_lambda";
  public static final String AWS_ELASTIC_BEANSTALK = "aws_elastic_beanstalk";
  // copied from HttpIncubatingAttributes
  private static final AttributeKey<String> HTTP_HOST = stringKey("http.host");
  private static final AttributeKey<String> HTTP_METHOD = stringKey("http.method");
  private static final AttributeKey<String> HTTP_TARGET = stringKey("http.target");
  private static final AttributeKey<String> HTTP_URL = stringKey("http.url");
  // copied from NetIncubatingAttributes
  private static final AttributeKey<String> NET_HOST_NAME = stringKey("net.host.name");

  private static final Map<String, String> XRAY_CLOUD_PLATFORM;

  // _OTHER request method:
  // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/registry/attributes/http.md?plain=1#L96
  private static final String _OTHER_REQUEST_METHOD = "_OTHER";

  static {
    Map<String, String> xrayCloudPlatform = new HashMap<>();
    xrayCloudPlatform.put(AWS_EC2, "AWS::EC2::Instance");
    xrayCloudPlatform.put(AWS_ECS, "AWS::ECS::Container");
    xrayCloudPlatform.put(AWS_EKS, "AWS::EKS::Container");
    xrayCloudPlatform.put(AWS_ELASTIC_BEANSTALK, "AWS::ElasticBeanstalk::Environment");
    xrayCloudPlatform.put(AWS_LAMBDA, "AWS::Lambda::Function");
    XRAY_CLOUD_PLATFORM = Collections.unmodifiableMap(xrayCloudPlatform);
  }

  private final String clientId;
  private final String ruleName;
  private final String serviceName;
  private final Clock clock;
  private final Sampler reservoirSampler;
  private final long reservoirEndTimeNanos;
  private final double fixedRate;
  private final Sampler fixedRateSampler;
  private final boolean borrowing;

  // Adaptive sampling related configs
  private final boolean hasBoost;
  private final double boostedFixedRate;
  private final Long boostEndTimeNanos;
  private final Sampler boostedFixedRateSampler;

  private final Map<String, Matcher> attributeMatchers;
  private final Matcher urlPathMatcher;
  private final Matcher serviceNameMatcher;
  private final Matcher httpMethodMatcher;
  private final Matcher hostMatcher;
  private final Matcher serviceTypeMatcher;
  private final Matcher resourceArnMatcher;

  private final Statistics statistics;

  private final long nextSnapshotTimeNanos;

  SamplingRuleApplier(
      String clientId,
      GetSamplingRulesResponse.SamplingRule rule,
      @Nullable String serviceName,
      Clock clock) {
    this.clientId = clientId;
    this.clock = clock;
    String ruleName = rule.getRuleName();
    if (ruleName == null) {
      // The AWS API docs mark this as an optional field but in practice it seems to always be
      // present, and sampling
      // targets could not be computed without it. For now provide an arbitrary fallback just in
      // case the AWS API docs
      // are correct.
      ruleName = "default";
    }
    this.ruleName = ruleName;

    this.serviceName = serviceName == null ? "default" : serviceName;

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
    fixedRate = rule.getFixedRate();
    fixedRateSampler = createFixedRate(fixedRate);

    // Check if the rule has a sampling rate boost option
    hasBoost = rule.getSamplingRateBoost() != null;

    boostedFixedRate = fixedRate;
    boostedFixedRateSampler = createFixedRate(fixedRate);
    boostEndTimeNanos = clock.nanoTime();

    if (rule.getAttributes().isEmpty()) {
      attributeMatchers = Collections.emptyMap();
    } else {
      attributeMatchers =
          rule.getAttributes().entrySet().stream()
              .collect(toMap(Map.Entry::getKey, e -> toMatcher(e.getValue())));
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
      String serviceName,
      Clock clock,
      Sampler reservoirSampler,
      long reservoirEndTimeNanos,
      double fixedRate,
      Sampler fixedRateSampler,
      boolean borrowing,
      double boostedFixedRate,
      Long boostEndTimeNanos,
      boolean hasBoost,
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
    this.serviceName = serviceName;
    this.clock = clock;
    this.reservoirSampler = reservoirSampler;
    this.reservoirEndTimeNanos = reservoirEndTimeNanos;
    this.fixedRate = fixedRate;
    this.fixedRateSampler = fixedRateSampler;
    this.borrowing = borrowing;
    this.boostedFixedRate = boostedFixedRate;
    this.boostEndTimeNanos = boostEndTimeNanos;
    this.hasBoost = hasBoost;
    this.attributeMatchers = attributeMatchers;
    this.urlPathMatcher = urlPathMatcher;
    this.serviceNameMatcher = serviceNameMatcher;
    this.httpMethodMatcher = httpMethodMatcher;
    this.hostMatcher = hostMatcher;
    this.serviceTypeMatcher = serviceTypeMatcher;
    this.resourceArnMatcher = resourceArnMatcher;
    this.statistics = statistics;
    this.nextSnapshotTimeNanos = nextSnapshotTimeNanos;
    this.boostedFixedRateSampler = createFixedRate(this.boostedFixedRate);
  }

  @SuppressWarnings("deprecation") // TODO
  boolean matches(Attributes attributes, Resource resource) {
    int matchedAttributes = 0;

    String httpTarget = attributes.get(UrlAttributes.URL_PATH);
    if (httpTarget == null) {
      httpTarget = attributes.get(HTTP_TARGET);
    }

    String httpUrl = attributes.get(UrlAttributes.URL_FULL);
    if (httpUrl == null) {
      httpUrl = attributes.get(HTTP_URL);
    }

    String httpMethod = attributes.get(HttpAttributes.HTTP_REQUEST_METHOD);
    if (httpMethod == null) {
      httpMethod = attributes.get(HTTP_METHOD);
    }

    if (httpMethod != null && httpMethod.equals(_OTHER_REQUEST_METHOD)) {
      httpMethod = attributes.get(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL);
    }

    String host = attributes.get(ServerAttributes.SERVER_ADDRESS);
    if (host == null) {
      host = attributes.get(NET_HOST_NAME);
      if (host == null) {
        host = attributes.get(HTTP_HOST);
      }
    }

    for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
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

    // URL Path may be in either http.target or http.url
    if (httpTarget == null && httpUrl != null) {
      int schemeEndIndex = httpUrl.indexOf("://");
      // Per spec, http.url is always populated with scheme://host/target. If scheme doesn't
      // match, assume it's bad instrumentation and ignore.
      if (schemeEndIndex > 0) {
        int pathIndex = httpUrl.indexOf('/', schemeEndIndex + "://".length());
        if (pathIndex < 0) {
          // No path, equivalent to root path.
          httpTarget = "/";
        } else {
          httpTarget = httpUrl.substring(pathIndex);
        }
      }
    }

    return urlPathMatcher.matches(httpTarget)
        && serviceNameMatcher.matches(resource.getAttribute(SERVICE_NAME))
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
    // Only emit statistics for spans for which a sampling decision is being made actively
    // i.e. The root span in a call chain
    boolean shouldCount = !Span.fromContext(parentContext).getSpanContext().isValid();
    // Incrementing requests first ensures sample / borrow rate are positive.
    if (shouldCount) {
      statistics.requests.increment();
    }
    boolean reservoirExpired = clock.nanoTime() >= reservoirEndTimeNanos;
    SamplingResult result =
        !reservoirExpired
            ? reservoirSampler.shouldSample(
                parentContext, traceId, name, spanKind, attributes, parentLinks)
            : SamplingResult.create(SamplingDecision.DROP);
    if (result.getDecision() != SamplingDecision.DROP) {
      // We use the result from the reservoir sampler if it worked.
      if (shouldCount) {
        if (borrowing) {
          statistics.borrowed.increment();
        }
        statistics.sampled.increment();
      }
      return result;
    }

    if (clock.nanoTime() < boostEndTimeNanos) {
      result =
          boostedFixedRateSampler.shouldSample(
              parentContext, traceId, name, spanKind, attributes, parentLinks);
    } else {
      result =
          fixedRateSampler.shouldSample(
              parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    if (shouldCount && result.getDecision() != SamplingDecision.DROP) {
      statistics.sampled.increment();
    }
    return result;
  }

  void countTrace() {
    statistics.traces.increment();
  }

  void countAnomalyTrace(ReadableSpan span) {
    statistics.anomalies.increment();

    if (span.getSpanContext().isSampled()) {
      statistics.anomaliesSampled.increment();
    }
  }

  @Nullable
  SamplingRuleStatisticsSnapshot snapshot(Date now) {
    if (clock.nanoTime() < nextSnapshotTimeNanos) {
      return null;
    }
    long totalCount = statistics.requests.sumThenReset();
    long sampledCount = statistics.sampled.sumThenReset();
    long borrowCount = statistics.borrowed.sumThenReset();
    long traceCount = statistics.traces.sumThenReset();
    long anomalyCount = statistics.anomalies.sumThenReset();
    long sampledAnomalyCount = statistics.anomaliesSampled.sumThenReset();
    SamplingStatisticsDocument samplingStatistics =
        SamplingStatisticsDocument.newBuilder()
            .setClientId(clientId)
            .setRuleName(ruleName)
            .setTimestamp(now)
            // Resetting requests first ensures that sample / borrow rate are positive after the
            // reset.
            // Snapshotting is not concurrent so this ensures they are always positive.
            .setRequestCount(totalCount)
            .setSampledCount(sampledCount)
            .setBorrowCount(borrowCount)
            .build();
    SamplingBoostStatisticsDocument boostDoc =
        SamplingBoostStatisticsDocument.newBuilder()
            .setRuleName(ruleName)
            .setServiceName(serviceName)
            .setTimestamp(now)
            .setTotalCount(traceCount)
            .setAnomalyCount(anomalyCount)
            .setSampledAnomalyCount(sampledAnomalyCount)
            .build();
    return new SamplingRuleStatisticsSnapshot(samplingStatistics, boostDoc);
  }

  long getNextSnapshotTimeNanos() {
    return nextSnapshotTimeNanos;
  }

  // currentNanoTime is passed in to ensure all uses of withTarget are used with the same baseline
  // time reference
  SamplingRuleApplier withTarget(SamplingTargetDocument target, Date now, long currentNanoTime) {
    Sampler newFixedRateSampler = createFixedRate(target.getFixedRate());
    Sampler newReservoirSampler = Sampler.alwaysOff();
    long newReservoirEndTimeNanos = currentNanoTime;
    // Not well documented but a quota should always come with a TTL
    if (target.getReservoirQuota() != null && target.getReservoirQuotaTtl() != null) {
      newReservoirSampler = createRateLimited(target.getReservoirQuota());
      newReservoirEndTimeNanos =
          currentNanoTime
              + Duration.between(now.toInstant(), target.getReservoirQuotaTtl().toInstant())
                  .toNanos();
    }
    long intervalNanos =
        target.getIntervalSecs() != null
            ? SECONDS.toNanos(target.getIntervalSecs())
            : AwsXrayRemoteSampler.DEFAULT_TARGET_INTERVAL_NANOS;
    long newNextSnapshotTimeNanos = currentNanoTime + intervalNanos;

    double newBoostedFixedRate = fixedRate;
    long newBoostEndTimeNanos = currentNanoTime;
    if (target.getSamplingBoost() != null) {
      SamplingBoost samplingBoostMap = target.getSamplingBoost();
      if (samplingBoostMap != null
          && samplingBoostMap.getBoostRate() >= target.getFixedRate()
          && samplingBoostMap.getBoostRateTtl() != null) {
        newBoostedFixedRate = samplingBoostMap.getBoostRate();
        newBoostEndTimeNanos =
            currentNanoTime
                + Duration.between(now.toInstant(), samplingBoostMap.getBoostRateTtl().toInstant())
                    .toNanos();
      }
    }

    return new SamplingRuleApplier(
        clientId,
        ruleName,
        serviceName,
        clock,
        newReservoirSampler,
        newReservoirEndTimeNanos,
        fixedRate,
        newFixedRateSampler,
        /* borrowing= */ false,
        newBoostedFixedRate,
        newBoostEndTimeNanos,
        hasBoost,
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
        serviceName,
        clock,
        reservoirSampler,
        reservoirEndTimeNanos,
        fixedRate,
        fixedRateSampler,
        borrowing,
        boostedFixedRate,
        boostEndTimeNanos,
        hasBoost,
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

  // For testing
  String getServiceName() {
    return serviceName;
  }

  boolean hasBoost() {
    return hasBoost;
  }

  @Nullable
  private static String getArn(Attributes attributes, Resource resource) {
    String arn = resource.getAttributes().get(AWS_ECS_CONTAINER_ARN);
    if (arn != null) {
      return arn;
    }
    String cloudPlatform = resource.getAttributes().get(CLOUD_PLATFORM);
    if (AWS_LAMBDA.equals(cloudPlatform)) {
      return getLambdaArn(attributes, resource);
    }
    return null;
  }

  @Nullable
  private static String getLambdaArn(Attributes attributes, Resource resource) {
    String arn = resource.getAttributes().get(CLOUD_RESOURCE_ID);
    if (arn != null) {
      return arn;
    }
    return attributes.get(CLOUD_RESOURCE_ID);
  }

  @Nullable
  private static String getServiceType(Resource resource) {
    String cloudPlatform = resource.getAttributes().get(CLOUD_PLATFORM);
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

  private static Sampler createFixedRate(double rate) {
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
    final LongAdder traces = new LongAdder();
    final LongAdder anomalies = new LongAdder();
    final LongAdder anomaliesSampled = new LongAdder();
  }

  static class SamplingRuleStatisticsSnapshot {
    final SamplingStatisticsDocument statisticsDocument;
    final SamplingBoostStatisticsDocument boostStatisticsDocument;

    // final SamplingBoostStatisticsDocument boostStatisticsDocument;

    SamplingRuleStatisticsSnapshot(
        SamplingStatisticsDocument statisticsDocument,
        SamplingBoostStatisticsDocument boostStatisticsDocument) {
      this.statisticsDocument = statisticsDocument;
      this.boostStatisticsDocument = boostStatisticsDocument;
    }

    SamplingStatisticsDocument getStatisticsDocument() {
      return statisticsDocument;
    }

    SamplingBoostStatisticsDocument getBoostStatisticsDocument() {
      return boostStatisticsDocument;
    }
  }
}
