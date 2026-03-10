/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.sampler;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DelegatingSamplerTest {

  @Test
  void defaultDelegateIsAlwaysOn() {
    DelegatingSampler sampler = new DelegatingSampler();

    SamplingDecision decision = doSample(sampler);

    assertThat(decision).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void setDelegateNullFallsBackToAlwaysOn() {
    DelegatingSampler sampler = new DelegatingSampler(Sampler.alwaysOff());

    sampler.setDelegate(null);

    SamplingDecision decision = doSample(sampler);
    assertThat(decision).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void shouldSampleDelegatesToCurrentSamplerAfterUpdate() {
    DelegatingSampler sampler = new DelegatingSampler(Sampler.alwaysOff());

    assertThat(doSample(sampler)).isEqualTo(SamplingDecision.DROP);

    sampler.setDelegate(Sampler.alwaysOn());

    assertThat(doSample(sampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  private static SamplingDecision doSample(DelegatingSampler sampler) {
    SamplingResult result =
        sampler.shouldSample(
            Context.root(),
            "00000000000000000000000000000001",
            "test-span",
            SpanKind.INTERNAL,
            Attributes.empty(),
            Collections.emptyList());
    return result.getDecision();
  }
}
