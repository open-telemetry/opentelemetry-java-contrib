/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ConsistentSamplerTest {

  private static class Input {
    private static final String traceId = "00112233445566778800000000000000";
    private static final String spanId = "0123456789abcdef";
    private static final String name = "name";
    private static final SpanKind spanKind = SpanKind.SERVER;
    private static final Attributes attributes = Attributes.empty();
    private static final List<LinkData> parentLinks = Collections.emptyList();
    private boolean parentSampled = true;

    private OptionalLong parentThreshold = OptionalLong.empty();
    private OptionalLong parentRandomValue = OptionalLong.empty();

    public void setParentSampled(boolean parentSampled) {
      this.parentSampled = parentSampled;
    }

    public void setParentThreshold(long parentThreshold) {
      assertThat(parentThreshold).isBetween(0L, 0xffffffffffffffL);
      this.parentThreshold = OptionalLong.of(parentThreshold);
    }

    public void setParentRandomValue(long parentRandomValue) {
      assertThat(parentRandomValue).isBetween(0L, 0xffffffffffffffL);
      this.parentRandomValue = OptionalLong.of(parentRandomValue);
    }

    public Context getParentContext() {
      return createParentContext(
          traceId, spanId, parentThreshold, parentRandomValue, parentSampled);
    }

    public static String getTraceId() {
      return traceId;
    }

    public static String getName() {
      return name;
    }

    public static SpanKind getSpanKind() {
      return spanKind;
    }

    public static Attributes getAttributes() {
      return attributes;
    }

    public static List<LinkData> getParentLinks() {
      return parentLinks;
    }
  }

  private static class Output {

    private final SamplingResult samplingResult;
    private final Context parentContext;

    Output(SamplingResult samplingResult, Context parentContext) {
      this.samplingResult = samplingResult;
      this.parentContext = parentContext;
    }

    boolean getSampledFlag() {
      return SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision());
    }

    OptionalLong getThreshold() {
      Span parentSpan = Span.fromContext(parentContext);
      OtelTraceState otelTraceState =
          OtelTraceState.parse(
              samplingResult
                  .getUpdatedTraceState(parentSpan.getSpanContext().getTraceState())
                  .get(OtelTraceState.TRACE_STATE_KEY));
      return otelTraceState.hasValidThreshold()
          ? OptionalLong.of(otelTraceState.getThreshold())
          : OptionalLong.empty();
    }

    OptionalLong getRandomValue() {
      Span parentSpan = Span.fromContext(parentContext);
      OtelTraceState otelTraceState =
          OtelTraceState.parse(
              samplingResult
                  .getUpdatedTraceState(parentSpan.getSpanContext().getTraceState())
                  .get(OtelTraceState.TRACE_STATE_KEY));
      return otelTraceState.hasValidRandomValue()
          ? OptionalLong.of(otelTraceState.getRandomValue())
          : OptionalLong.empty();
    }
  }

  private static TraceState createTraceState(OptionalLong threshold, OptionalLong randomValue) {
    OtelTraceState state = OtelTraceState.parse("");
    threshold.ifPresent(x -> state.setThreshold(x));
    randomValue.ifPresent(x -> state.setRandomValue(x));
    return TraceState.builder().put(OtelTraceState.TRACE_STATE_KEY, state.serialize()).build();
  }

  private static Context createParentContext(
      String traceId,
      String spanId,
      OptionalLong threshold,
      OptionalLong randomValue,
      boolean sampled) {
    TraceState parentTraceState = createTraceState(threshold, randomValue);
    TraceFlags traceFlags = sampled ? TraceFlags.getSampled() : TraceFlags.getDefault();
    SpanContext parentSpanContext =
        SpanContext.create(traceId, spanId, traceFlags, parentTraceState);
    Span parentSpan = Span.wrap(parentSpanContext);
    return parentSpan.storeInContext(Context.root());
  }

  private static Output sample(Input input, ConsistentSampler sampler) {

    Context parentContext = input.getParentContext();
    SamplingResult samplingResult =
        sampler.shouldSample(
            parentContext,
            Input.getTraceId(),
            Input.getName(),
            Input.getSpanKind(),
            Input.getAttributes(),
            Input.getParentLinks());
    return new Output(samplingResult, parentContext);
  }

  @Test
  void testMinThresholdWithoutParentRandomValue() {

    Input input = new Input();

    ConsistentSampler sampler = ConsistentSampler.alwaysOn();

    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    assertThat(output.getThreshold()).hasValue(0);
    assertThat(output.getRandomValue()).isNotPresent();
    assertThat(output.getSampledFlag()).isTrue();
  }

  @Test
  void testMinThresholdWithParentRandomValue() {

    long parentRandomValue = 0x7f99aa40c02744L;

    Input input = new Input();
    input.setParentRandomValue(parentRandomValue);

    ConsistentSampler sampler = ConsistentSampler.alwaysOn();

    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    assertThat(output.getThreshold()).hasValue(0);
    assertThat(output.getRandomValue()).hasValue(parentRandomValue);
    assertThat(output.getSampledFlag()).isTrue();
  }

  @Test
  void testMaxThreshold() {

    Input input = new Input();

    ConsistentSampler sampler = new ConsistentFixedThresholdSampler(getMaxThreshold());

    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.DROP);
    assertThat(output.getThreshold()).isEmpty();
    assertThat(output.getRandomValue()).isNotPresent();
    assertThat(output.getSampledFlag()).isFalse();
  }

  @Test
  void testHalfThresholdNotSampled() {

    Input input = new Input();
    input.setParentRandomValue(0x7FFFFFFFFFFFFFL);

    ConsistentSampler sampler = new ConsistentFixedThresholdSampler(0x80000000000000L);

    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.DROP);
    assertThat(output.getThreshold()).isNotPresent();
    assertThat(output.getRandomValue()).hasValue(0x7FFFFFFFFFFFFFL);
    assertThat(output.getSampledFlag()).isFalse();
  }

  @Test
  void testHalfThresholdSampled() {

    Input input = new Input();
    input.setParentRandomValue(0x80000000000000L);

    ConsistentSampler sampler = new ConsistentFixedThresholdSampler(0x80000000000000L);

    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    assertThat(output.getThreshold()).hasValue(0x80000000000000L);
    assertThat(output.getRandomValue()).hasValue(0x80000000000000L);
    assertThat(output.getSampledFlag()).isTrue();
  }

  @Test
  void testParentViolatingInvariant() {

    Input input = new Input();
    input.setParentThreshold(0x80000000000000L);
    input.setParentRandomValue(0x80000000000000L);
    input.setParentSampled(false);

    ConsistentSampler sampler = new ConsistentFixedThresholdSampler(0x0);
    Output output = sample(input, sampler);

    assertThat(output.samplingResult.getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);

    assertThat(output.getThreshold()).hasValue(0x0L);
    assertThat(output.getRandomValue()).hasValue(0x80000000000000L);
    assertThat(output.getSampledFlag()).isTrue();
  }
}
