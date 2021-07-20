/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.GetSamplingRulesResponse.SamplingRule;
import io.opentelemetry.contrib.awsxray.GetSamplingTargetsResponse.SamplingTargetDocument;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.time.TestClock;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class XrayRulesSamplerTest {

  @Test
  void updateTargets() {
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
            "cat-rule",
            "cat-service",
            "*",
            "*",
            1);
    SamplingRule rule2 =
        SamplingRule.create(
            Collections.emptyMap(),
            0.0,
            "*",
            "*",
            2,
            1,
            "*",
            "*",
            "dog-rule",
            "dog-service",
            "*",
            "*",
            1);
    SamplingRule rule3 =
        SamplingRule.create(
            Collections.emptyMap(),
            1.0,
            "*",
            "*",
            3,
            1,
            "*",
            "*",
            "bat-rule",
            "*-service",
            "*",
            "*",
            1);
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
            1);

    TestClock clock = TestClock.create();
    XrayRulesSampler sampler =
        new XrayRulesSampler(
            "CLIENT_ID",
            Resource.getDefault(),
            clock,
            Sampler.alwaysOn(),
            Arrays.asList(rule1, rule4, rule3, rule2));

    assertThat(doSample(sampler, "cat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "cat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "dog-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "dog-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(sampler, "bat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "bat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "unknown"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));

    Instant now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);
    assertThat(sampler.nextTargetFetchTimeNanos()).isEqualTo(clock.nanoTime());
    clock.advance(Duration.ofSeconds(10));
    now = Instant.ofEpochSecond(0, clock.now());
    assertThat(sampler.snapshot(Date.from(now))).hasSize(4);

    SamplingTargetDocument catTarget =
        SamplingTargetDocument.create(0.0, 10, null, null, "cat-rule");

    SamplingTargetDocument batTarget =
        SamplingTargetDocument.create(0.0, 5, null, null, "bat-rule");

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
        .isEqualTo(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
    assertThat(doSample(sampler, "dog-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(sampler, "unknown"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    // Targets overridden to always drop.
    assertThat(doSample(sampler, "cat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));
    assertThat(doSample(sampler, "bat-service"))
        .isEqualTo(SamplingResult.create(SamplingDecision.DROP));

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

  private SamplingResult doSample(Sampler sampler, String name) {
    return sampler.shouldSample(
        Context.current(),
        TraceId.fromLongs(1, 2),
        name,
        SpanKind.CLIENT,
        Attributes.empty(),
        Collections.emptyList());
  }
}
