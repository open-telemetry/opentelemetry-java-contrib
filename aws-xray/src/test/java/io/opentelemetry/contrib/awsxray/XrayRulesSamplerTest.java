/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingRulesResponse.SamplingRateBoost;
import io.opentelemetry.contrib.awsxray.GetSamplingRulesResponse.SamplingRule;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.time.TestClock;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class XrayRulesSamplerTest {

  private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
  private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");

  @Test
  void updateTargets() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.singletonMap("test", "cat-service"),
            1.0,
            "*",
            "*",
            1,
            1,
            "*",
            "*",
            "cat-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule2 =
        SamplingRule.create(
            Collections.singletonMap("test", "dog-service"),
            0.0,
            "*",
            "*",
            2,
            1,
            "*",
            "*",
            "dog-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule3 =
        SamplingRule.create(
            Collections.singletonMap("test", "*-service"),
            1.0,
            "*",
            "*",
            3,
            1,
            "*",
            "*",
            "bat-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule4 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            4,
            0,
            "*",
            "*",
            "default-rule",
            "*",
            "*",
            "*",
            1,
            null);

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1, rule4, rule3, rule2),
            null);

    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "cat-rule"));
    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "cat-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "dog-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "dog-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "bat-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "bat-rule"));
    assertThat(doSample(sampler, "unknown"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "default-rule"));

    Instant now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);
    assertThat(sampler.nextTargetFetchTimeNanos()).isEqualTo(clock.nanoTime());
    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);

    SamplingTargetDocument catTarget =
        SamplingTargetDocument.create(0.0, 10, null, null, null, "cat-rule");

    SamplingTargetDocument batTarget =
        SamplingTargetDocument.create(0.0, 5, null, null, null, "bat-rule");

    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    Map<String, SamplingTargetDocument> targets = new HashMap<>();
    targets.put("cat-rule", catTarget);
    targets.put("bat-rule", batTarget);
    sampler =
        sampler.withTargets(
            targets,
            Stream.of("cat-rule", "bat-rule", "dog-rule", "default-rule")
                .collect(Collectors.toSet()),
            Date.from(now));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE, Attributes.empty(), "dog-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "dog-rule"));
    assertThat(doSample(sampler, "unknown"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "default-rule"));
    // Targets overridden to always drop.
    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "cat-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(AwsSamplingResult.create(SamplingDecision.DROP, Attributes.empty(), "bat-rule"));

    // Minimum is batTarget, 5s from now
    assertThat(sampler.nextTargetFetchTimeNanos())
        .isEqualTo(clock.nanoTime() + TimeUnit.SECONDS.toNanos(5));

    assertThat(sampler.snapshot(Date.from(now))).isEmpty();
    clock.advance(Duration.ofSeconds(5));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(1);
    clock.advance(Duration.ofSeconds(5));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);
  }

  @Test
  void updateTargetsWithLocalAdaptiveSamplingConfig() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.singletonMap("test", "cat-service"),
            1.0,
            "*",
            "*",
            1,
            1,
            "*",
            "*",
            "cat-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule2 =
        SamplingRule.create(
            Collections.singletonMap("test", "dog-service"),
            0.0,
            "*",
            "*",
            2,
            1,
            "*",
            "*",
            "dog-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule3 =
        SamplingRule.create(
            Collections.singletonMap("test", "*-service"),
            1.0,
            "*",
            "*",
            3,
            1,
            "*",
            "*",
            "bat-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule4 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            4,
            0,
            "*",
            "*",
            "default-rule",
            "*",
            "*",
            "*",
            1,
            null);
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(2)
                    .build())
            .build();

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1, rule4, rule3, rule2),
            config);

    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "cat-rule")
                    .build(),
                "cat-rule"));
    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "cat-rule")
                    .build(),
                "cat-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "dog-rule")
                    .build(),
                "dog-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "dog-rule")
                    .build(),
                "dog-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "bat-rule")
                    .build(),
                "bat-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "bat-rule")
                    .build(),
                "bat-rule"));
    assertThat(doSample(sampler, "unknown"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "default-rule")
                    .build(),
                "default-rule"));

    Instant now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);
    assertThat(sampler.nextTargetFetchTimeNanos()).isEqualTo(clock.nanoTime());
    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);

    SamplingTargetDocument catTarget =
        SamplingTargetDocument.create(0.0, 10, null, null, null, "cat-rule");

    SamplingTargetDocument batTarget =
        SamplingTargetDocument.create(0.0, 5, null, null, null, "bat-rule");

    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    Map<String, SamplingTargetDocument> targets = new HashMap<>();
    targets.put("cat-rule", catTarget);
    targets.put("bat-rule", batTarget);
    sampler =
        sampler.withTargets(
            targets,
            Stream.of("cat-rule", "bat-rule", "dog-rule", "default-rule")
                .collect(Collectors.toSet()),
            Date.from(now));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.RECORD_AND_SAMPLE,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "dog-rule")
                    .build(),
                "dog-rule"));
    assertThat(doSample(sampler, "dog-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "dog-rule")
                    .build(),
                "dog-rule"));
    assertThat(doSample(sampler, "unknown"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "default-rule")
                    .build(),
                "default-rule"));
    // Targets overridden to always drop.
    assertThat(doSample(sampler, "cat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "cat-rule")
                    .build(),
                "cat-rule"));
    assertThat(doSample(sampler, "bat-service"))
        .usingRecursiveComparison()
        .isEqualTo(
            AwsSamplingResult.create(
                SamplingDecision.DROP,
                Attributes.builder()
                    .put(XrayRulesSampler.AWS_XRAY_SAMPLING_RULE, "bat-rule")
                    .build(),
                "bat-rule"));

    // Minimum is batTarget, 5s from now
    assertThat(sampler.nextTargetFetchTimeNanos())
        .isEqualTo(clock.nanoTime() + TimeUnit.SECONDS.toNanos(5));

    assertThat(sampler.snapshot(Date.from(now))).isEmpty();
    clock.advance(Duration.ofSeconds(5));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(1);
    clock.advance(Duration.ofSeconds(5));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);
  }

  @Test
  void noAdaptiveSamplingUsesNoSpace() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.singletonMap("test", "cat-service"),
            1.0,
            "*",
            "*",
            1,
            1,
            "*",
            "*",
            "cat-rule",
            "*",
            "*",
            "*",
            1,
            null);

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            null);

    LongAdder exportCounter = new LongAdder();
    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    SpanData spanDataMock = mock(SpanData.class);
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.increment();
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(sampler.getAnomalyTracesSet().isEmpty()).isEqualTo(true);
  }

  @Test
  void recordErrors() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.singletonMap("test", "cat-service"),
            1.0,
            "*",
            "*",
            1,
            1,
            "*",
            "*",
            "cat-rule",
            "*",
            "*",
            "*",
            1,
            null);
    SamplingRule rule2 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            4,
            0,
            "*",
            "*",
            "default-rule",
            "*",
            "*",
            "*",
            1,
            SamplingRateBoost.create(1, 300));
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(2)
                    .build())
            .setAnomalyConditions(
                Arrays.asList(
                    AwsXrayAdaptiveSamplingConfig.AnomalyConditions.builder()
                        .setErrorCodeRegex("^500$")
                        .setUsage(AwsXrayAdaptiveSamplingConfig.UsageType.BOTH)
                        .build()))
            .build();

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1, rule2),
            config);

    Instant now = Instant.ofEpochSecond(0, clock.now());

    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID", "SPAN_ID", TraceFlags.getDefault(), TraceState.getDefault()));
    SpanData spanDataMock = mock(SpanData.class);
    Attributes attributesMock = mock(Attributes.class);
    when(spanDataMock.getTraceId()).thenReturn("TRACE_ID");
    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(500L);
    LongAdder exportCounter = new LongAdder();
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.increment();

    // First span should be captured, second should be rate limited
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    // Only first span captured due to rate limiting
    assertThat(exportCounter.sumThenReset()).isEqualTo(2L);

    List<SamplingRuleApplier.SamplingRuleStatisticsSnapshot> snapshot =
        sampler.snapshot(Date.from(now));

    // Rules are ordered by priority, so cat-rule is first
    assertThat(snapshot.get(0).getBoostStatisticsDocument().getTotalCount()).isEqualTo(0);
    assertThat(snapshot.get(0).getBoostStatisticsDocument().getAnomalyCount()).isEqualTo(0);

    assertThat(snapshot.get(0).getBoostStatisticsDocument().getSampledAnomalyCount()).isEqualTo(0);
    assertThat(snapshot.get(1).getBoostStatisticsDocument().getTotalCount()).isEqualTo(3);
    assertThat(snapshot.get(1).getBoostStatisticsDocument().getAnomalyCount()).isEqualTo(3);

    assertThat(snapshot.get(1).getBoostStatisticsDocument().getSampledAnomalyCount()).isEqualTo(0);

    // Mock trace coming from upstream service where it was sampled by cat-rule
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID",
                "SPAN_ID",
                TraceFlags.getDefault(),
                TraceState.builder()
                    .put(AwsSamplingResult.AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY, "cat-rule")
                    .build()));

    // When we adapt sampling, we should see this rule and report to it even though it doesn't
    // match
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);

    // Ensure snapshot shows correctly saved statistics
    snapshot = sampler.snapshot(Date.from(now));
    // cat-rule has no boost config and therefore records no statistics
    assertThat(snapshot.get(0).getBoostStatisticsDocument().getTotalCount()).isEqualTo(0);
    assertThat(snapshot.get(0).getBoostStatisticsDocument().getAnomalyCount()).isEqualTo(0);
    assertThat(snapshot.get(0).getBoostStatisticsDocument().getSampledAnomalyCount()).isEqualTo(0);
    assertThat(snapshot.get(1).getBoostStatisticsDocument().getTotalCount()).isEqualTo(0);
    assertThat(snapshot.get(1).getBoostStatisticsDocument().getAnomalyCount()).isEqualTo(0);
    assertThat(snapshot.get(1).getBoostStatisticsDocument().getSampledAnomalyCount()).isEqualTo(0);
    assertThat(sampler.getAnomalyTracesSet().isEmpty()).isEqualTo(true);
  }

  @Test
  void setAdaptiveSamplingConfigTwice() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.emptyMap(),
            1.0,
            "*",
            "*",
            1,
            1,
            "*",
            "*",
            "test-rule",
            "*",
            "*",
            "*",
            1,
            null);

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            null);

    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder().setVersion(1.0).build();
    sampler.setAdaptiveSamplingConfig(config);
    assertThrows(IllegalStateException.class, () -> sampler.setAdaptiveSamplingConfig(config));
  }

  @Test
  void captureErrorBasedOnErrorCodeRegex() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            1,
            0,
            "*",
            "*",
            "test-rule",
            "*",
            "*",
            "*",
            1,
            SamplingRateBoost.create(1, 300));

    TestClock clock = TestClock.create();
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(2)
                    .build())
            .setAnomalyConditions(
                Arrays.asList(
                    AwsXrayAdaptiveSamplingConfig.AnomalyConditions.builder()
                        .setErrorCodeRegex("^456$")
                        .setUsage(AwsXrayAdaptiveSamplingConfig.UsageType.BOTH)
                        .build()))
            .build();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            config);

    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID", "SPAN_ID", TraceFlags.getDefault(), TraceState.getDefault()));
    when(readableSpanMock.getAttribute(any())).thenReturn("test-operation");
    when(readableSpanMock.getLatencyNanos()).thenReturn(1L);

    SpanData spanDataMock = mock(SpanData.class);
    Attributes attributesMock = mock(Attributes.class);
    when(spanDataMock.getTraceId()).thenReturn("TRACE_ID");
    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(456L);

    LongAdder exportCounter = new LongAdder();
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.increment();

    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(2L);
  }

  @Test
  void captureErrorBasedOnHighLatency() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            1,
            0,
            "*",
            "*",
            "test-rule",
            "*",
            "*",
            "*",
            1,
            SamplingRateBoost.create(1, 300));

    TestClock clock = TestClock.create();
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(2)
                    .build())
            .setAnomalyConditions(
                Arrays.asList(
                    AwsXrayAdaptiveSamplingConfig.AnomalyConditions.builder()
                        .setHighLatencyMs(100L)
                        .setUsage(AwsXrayAdaptiveSamplingConfig.UsageType.ERROR_SPAN_CAPTURE)
                        .build()))
            .build();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            config);

    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID", "SPAN_ID", TraceFlags.getDefault(), TraceState.getDefault()));
    when(readableSpanMock.getAttribute(any())).thenReturn("test-operation");
    when(readableSpanMock.getLatencyNanos()).thenReturn(300_000_000L); // 300 ms

    SpanData spanDataMock = mock(SpanData.class);
    Attributes attributesMock = mock(Attributes.class);
    when(spanDataMock.getTraceId()).thenReturn("TRACE_ID");
    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(200L);

    LongAdder exportCounter = new LongAdder();
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.add(1);

    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(2L);
  }

  @Test
  void captureErrorBasedOnErroCodeAndLatency() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            1,
            0,
            "*",
            "*",
            "test-rule",
            "*",
            "*",
            "*",
            1,
            SamplingRateBoost.create(1, 300));

    TestClock clock = TestClock.create();
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(2)
                    .build())
            .setAnomalyConditions(
                Arrays.asList(
                    AwsXrayAdaptiveSamplingConfig.AnomalyConditions.builder()
                        .setErrorCodeRegex("^456$")
                        .setHighLatencyMs(100L)
                        .setUsage(AwsXrayAdaptiveSamplingConfig.UsageType.ERROR_SPAN_CAPTURE)
                        .build()))
            .build();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            config);

    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID", "SPAN_ID", TraceFlags.getDefault(), TraceState.getDefault()));
    when(readableSpanMock.getAttribute(any())).thenReturn("test-operation");
    when(readableSpanMock.getLatencyNanos()).thenReturn(300_000_000L); // 300 ms

    SpanData spanDataMock = mock(SpanData.class);
    Attributes attributesMock = mock(Attributes.class);
    when(spanDataMock.getTraceId()).thenReturn("TRACE_ID");
    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(200L);

    LongAdder exportCounter = new LongAdder();
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.add(1);

    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(0L);

    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(456L);
    when(readableSpanMock.getLatencyNanos()).thenReturn(1L);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(0L);

    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(456L);
    when(readableSpanMock.getLatencyNanos()).thenReturn(300_000_000L); // 300 ms
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(2L);
  }

  @Test
  void operationFilteringInAdaptSampling() {
    SamplingRule rule1 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            1,
            0,
            "*",
            "*",
            "test-rule",
            "*",
            "*",
            "*",
            1,
            SamplingRateBoost.create(1, 300));

    TestClock clock = TestClock.create();
    AwsXrayAdaptiveSamplingConfig config =
        AwsXrayAdaptiveSamplingConfig.builder()
            .setVersion(1.0)
            .setErrorCaptureLimit(
                AwsXrayAdaptiveSamplingConfig.ErrorCaptureLimit.builder()
                    .setErrorSpansPerSecond(10)
                    .build())
            .setAnomalyConditions(
                Arrays.asList(
                    AwsXrayAdaptiveSamplingConfig.AnomalyConditions.builder()
                        .setOperations(Arrays.asList("GET /api1", "GET /api2"))
                        .setErrorCodeRegex("^500$")
                        .setUsage(AwsXrayAdaptiveSamplingConfig.UsageType.ERROR_SPAN_CAPTURE)
                        .build()))
            .build();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1),
            config);

    ReadableSpan readableSpanMock = mock(ReadableSpan.class);
    when(readableSpanMock.getSpanContext())
        .thenReturn(
            SpanContext.create(
                "TRACE_ID", "SPAN_ID", TraceFlags.getDefault(), TraceState.getDefault()));
    when(readableSpanMock.getLatencyNanos()).thenReturn(1L);

    SpanData spanDataMock = mock(SpanData.class);
    Attributes attributesMock = mock(Attributes.class);
    when(spanDataMock.getTraceId()).thenReturn("TRACE_ID");
    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(attributesMock.get(HTTP_RESPONSE_STATUS_CODE)).thenReturn(500L);

    LongAdder exportCounter = new LongAdder();
    Consumer<ReadableSpan> stubbedConsumer = x -> exportCounter.increment();

    // Test matching operations
    when(attributesMock.get(URL_PATH)).thenReturn("/api1/ext");
    when(attributesMock.get(HTTP_METHOD)).thenReturn("GET");
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    when(attributesMock.get(URL_PATH)).thenReturn("/api2");
    when(attributesMock.get(HTTP_METHOD)).thenReturn("GET");
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sumThenReset()).isEqualTo(2L);

    // Test non-matching operation
    when(attributesMock.get(URL_PATH)).thenReturn("/api1/ext");
    when(attributesMock.get(HTTP_METHOD)).thenReturn("POST");
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    when(attributesMock.get(URL_PATH)).thenReturn("/non-matching");
    when(attributesMock.get(HTTP_METHOD)).thenReturn("GET");
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(0L);

    // Test aws.local.operation takes priority
    when(attributesMock.get(AwsAttributeKeys.AWS_LOCAL_OPERATION)).thenReturn("GET /api1");
    sampler.adaptSampling(readableSpanMock, spanDataMock, stubbedConsumer);
    assertThat(exportCounter.sum()).isEqualTo(1L);
  }

  private static SamplingResult doSample(Sampler sampler, String name) {
    return sampler.shouldSample(
        Context.current(),
        TraceId.fromLongs(1, 2),
        name,
        SpanKind.CLIENT,
        Attributes.of(AttributeKey.stringKey("test"), name),
        Collections.emptyList());
  }
}
