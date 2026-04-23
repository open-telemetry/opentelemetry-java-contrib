/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.INVALID_THRESHOLD;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ConsistentAnyOfTest {

  /** Test helper: a {@link ComposableSampler} that returns a fixed threshold. */
  static final class TestSampler implements ComposableSampler {
    private final long threshold;
    private final boolean thresholdReliable;

    TestSampler(long threshold, boolean thresholdReliable) {
      this.threshold = threshold;
      this.thresholdReliable = thresholdReliable;
    }

    @Override
    public SamplingIntent getSamplingIntent(
        Context parentContext,
        String traceId,
        String name,
        SpanKind spanKind,
        Attributes attributes,
        List<LinkData> parentLinks) {
      return SamplingIntent.create(
          threshold, thresholdReliable, Attributes.empty(), Function.identity());
    }

    @Override
    public String getDescription() {
      return "TestSampler";
    }
  }

  @Test
  void testMinimumThresholdWithAdjustedCount() {
    ComposableSampler delegate1 =
        new TestSampler(0x80000000000000L, /* thresholdReliable= */ false);
    ComposableSampler delegate2 = new TestSampler(0x30000000000000L, /* thresholdReliable= */ true);
    ComposableSampler delegate3 =
        new TestSampler(0xa0000000000000L, /* thresholdReliable= */ false);
    ComposableSampler delegate4 =
        new TestSampler(0x30000000000000L, /* thresholdReliable= */ false);

    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3, delegate4);
    SamplingIntent intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isThresholdReliable()).isTrue();

    sampler = ConsistentSampler.anyOf(delegate1, delegate4, delegate3, delegate2);
    intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isThresholdReliable()).isTrue();
  }

  @Test
  void testMinimumThresholdWithoutAdjustedCount() {
    ComposableSampler delegate1 = new TestSampler(0x80000000000000L, /* thresholdReliable= */ true);
    ComposableSampler delegate2 =
        new TestSampler(0x30000000000000L, /* thresholdReliable= */ false);
    ComposableSampler delegate3 = new TestSampler(0xa0000000000000L, /* thresholdReliable= */ true);

    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isThresholdReliable()).isFalse();
  }

  @Test
  void testAlwaysDrop() {
    ComposableSampler delegate1 = ComposableSampler.alwaysOff();
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1);
    SamplingIntent intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(INVALID_THRESHOLD);
    assertThat(intent.isThresholdReliable()).isFalse();
  }

  @Test
  void testSpanAttributesAdded() {
    AttributeKey<String> key1 = AttributeKey.stringKey("tag1");
    AttributeKey<String> key2 = AttributeKey.stringKey("tag2");
    AttributeKey<String> key3 = AttributeKey.stringKey("tag3");
    ComposableSampler delegate1 =
        new MarkingSampler(
            new TestSampler(0x30000000000000L, /* thresholdReliable= */ true), key1, "a");
    ComposableSampler delegate2 =
        new MarkingSampler(
            new TestSampler(0x50000000000000L, /* thresholdReliable= */ true), key2, "b");
    ComposableSampler delegate3 = new MarkingSampler(ComposableSampler.alwaysOff(), key3, "c");
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("a");
    assertThat(intent.getAttributes().get(key2)).isEqualTo("b");
    assertThat(intent.getAttributes().get(key3)).isEqualTo("c");
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isThresholdReliable()).isTrue();
  }

  @Test
  void testSpanAttributeOverride() {
    AttributeKey<String> key1 = AttributeKey.stringKey("shared");
    ComposableSampler delegate1 =
        new MarkingSampler(
            new TestSampler(0x30000000000000L, /* thresholdReliable= */ true), key1, "a");
    ComposableSampler delegate2 =
        new MarkingSampler(
            new TestSampler(0x50000000000000L, /* thresholdReliable= */ true), key1, "b");
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2);
    SamplingIntent intent = sampler.getSamplingIntent(null, "tid", "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("b");
  }
}
