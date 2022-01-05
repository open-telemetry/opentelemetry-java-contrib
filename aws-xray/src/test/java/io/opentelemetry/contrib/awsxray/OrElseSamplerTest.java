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
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrElseSamplerTest {

  @Test
  void firstWins() {
    Sampler sampler = new OrElseSampler(Sampler.alwaysOn(), Sampler.alwaysOff());
    assertThat(doSample(sampler).getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void fallsBackToSecond() {
    Sampler sampler = new OrElseSampler(Sampler.alwaysOff(), Sampler.alwaysOn());
    assertThat(doSample(sampler).getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    sampler = new OrElseSampler(Sampler.alwaysOff(), Sampler.alwaysOff());
    assertThat(doSample(sampler).getDecision()).isEqualTo(SamplingDecision.DROP);
  }

  private static SamplingResult doSample(Sampler sampler) {
    return sampler.shouldSample(
        Context.current(),
        TraceId.fromLongs(1, 2),
        "span",
        SpanKind.CLIENT,
        Attributes.empty(),
        Collections.emptyList());
  }
}
