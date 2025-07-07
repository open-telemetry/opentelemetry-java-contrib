/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

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
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
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
import javax.annotation.Nullable;

final class SamplingRuleApplier {

  // copied from AwsIncubatingAttributes
  private static final AttributeKey<String> AWS_ECS_CONTAINER_ARN =
      AttributeKey.stringKey("aws.ecs.container.arn");
  // copied from CloudIncubatingAttributes
  private static final AttributeKey<String> CLOUD_PLATFORM =
      AttributeKey.stringKey("cloud.platform");
  private static final AttributeKey<String> CLOUD_RESOURCE_ID =
      AttributeKey.stringKey("cloud.resource_id");
  // copied from CloudIncubatingAttributes.CloudPlatformIncubatingValues
  public static final String AWS_EC2 = "aws_ec2";
  public static final String AWS_ECS = "aws_ecs";
  public static final String AWS_EKS = "aws_eks";
  public static final String AWS_LAMBDA = "aws_lambda";
  public static final String AWS_ELASTIC_BEANSTALK = "aws_elastic_beanstalk";
  // copied from HttpIncubatingAttributes
  private static final AttributeKey<String> HTTP_HOST = AttributeKey.stringKey("http.host");
  private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
  private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
  private static final AttributeKey<String> HTTP_URL = AttributeKey.stringKey("http.url");
  // copied from NetIncubatingAttributes
  private static final AttributeKey<String> NET_HOST_NAME = AttributeKey.stringKey("net.host.name");

  private static final Map<String, String> XRAY_CLOUD_PLATFORM;

  // _OTHER request method: https://github.com/open-telemetry/semantic-conventions/blob/main/docs/registry/attributes/http.md?plain=1#L96
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

  @SuppressWarnings("deprecation") // TODO
  boolean matches(Attributes attributes, Resource resource) {
    int matchedAttributes = 0;
    String httpTarget = null;
    String httpUrl = null;
    String httpMethod = null;
    String host = null;

    for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
      if (entry.getKey().equals(HTTP_TARGET) || entry.getKey().equals(UrlAttributes.URL_PATH)) {
        httpTarget = (String) entry.getValue();
      } else if (entry.getKey().equals(HTTP_URL) || entry.getKey().equals(UrlAttributes.URL_FULL)) {
        httpUrl = (String) entry.getValue();
      } else if (entry.getKey().equals(HTTP_METHOD)
          || entry.getKey().equals(HttpAttributes.HTTP_REQUEST_METHOD)) {
        httpMethod = (String) entry.getValue();
        // according to semantic conventions, if the HTTP request method is not known to instrumentation
        // it must be set to _OTHER and the HTTP_REQUEST_METHOD_ORIGINAL should contain the original method
        if (httpMethod.equals(_OTHER_REQUEST_METHOD)) {
          httpMethod = attributes.get(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL);
        }
      } else if (entry.getKey().equals(NET_HOST_NAME) || (entry.getKey().equals(ServerAttributes.SERVER_ADDRESS))) {
        host = (String) entry.getValue();
      } else if (entry.getKey().equals(HTTP_HOST) || (entry.getKey().equals(ServerAttributes.SERVER_ADDRESS))) {
        // TODO (trask) remove support for deprecated http.host attribute
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
    // Incrementing requests first ensures sample / borrow rate are positive.
    statistics.requests.increment();
    boolean reservoirExpired = clock.nanoTime() >= reservoirEndTimeNanos;
    SamplingResult result =
        !reservoirExpired
            ? reservoirSampler.shouldSample(
                parentContext, traceId, name, spanKind, attributes, parentLinks)
            : SamplingResult.create(SamplingDecision.DROP);
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
    long newReservoirEndTimeNanos = clock.nanoTime();
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
    long newNextSnapshotTimeNanos = clock.nanoTime() + intervalNanos;

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
    return new RateLimitingSampler(numPerSecond, clock);
  }

  private static Sampler createFixedRate(double rate) {
    return Sampler.traceIdRatioBased(rate);
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
