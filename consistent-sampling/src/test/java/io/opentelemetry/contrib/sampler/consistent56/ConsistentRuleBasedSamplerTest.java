/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.jupiter.api.Test;

class ConsistentRuleBasedSamplerTest {

  @Test
  void testEmptySet() {
    ComposableSampler sampler = ConsistentSampler.ruleBased(SpanKind.SERVER);
    SamplingIntent intent =
        sampler.getSamplingIntent(null, "span_name", SpanKind.SERVER, null, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
  }

  private static Predicate matchSpanName(String nameToMatch) {
    return (parentContext, name, spanKind, attributes, parentLinks) -> {
      return nameToMatch.equals(name);
    };
  }

  @Test
  void testChoice() {
    // Testing the correct choice by checking both the returned threshold and the marking attribute

    AttributeKey<String> key1 = AttributeKey.stringKey("tag1");
    AttributeKey<String> key2 = AttributeKey.stringKey("tag2");
    AttributeKey<String> key3 = AttributeKey.stringKey("tag3");

    ComposableSampler delegate1 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x80000000000000L), key1, "a");
    ComposableSampler delegate2 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x50000000000000L), key2, "b");
    ComposableSampler delegate3 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x30000000000000L), key3, "c");

    ComposableSampler sampler =
        ConsistentSampler.ruleBased(
            null,
            PredicatedSampler.onMatch(matchSpanName("A"), delegate1),
            PredicatedSampler.onMatch(matchSpanName("B"), delegate2),
            PredicatedSampler.onMatch(matchSpanName("C"), delegate3));

    SamplingIntent intent;

    intent = sampler.getSamplingIntent(null, "A", SpanKind.CLIENT, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x80000000000000L);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("a");
    assertThat(intent.getAttributes().get(key2)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key3)).isEqualTo(null);

    intent = sampler.getSamplingIntent(null, "B", SpanKind.PRODUCER, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x50000000000000L);
    assertThat(intent.getAttributes().get(key1)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key2)).isEqualTo("b");
    assertThat(intent.getAttributes().get(key3)).isEqualTo(null);

    intent = sampler.getSamplingIntent(null, "C", SpanKind.SERVER, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.getAttributes().get(key1)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key2)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key3)).isEqualTo("c");

    intent = sampler.getSamplingIntent(null, "D", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
    assertThat(intent.getAttributes().get(key1)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key2)).isEqualTo(null);
    assertThat(intent.getAttributes().get(key3)).isEqualTo(null);
  }

  @Test
  void testSpanKindMatch() {
    ComposableSampler sampler =
        ConsistentSampler.ruleBased(
            SpanKind.CLIENT,
            PredicatedSampler.onMatch(Predicate.anySpan(), ConsistentSampler.alwaysOn()));

    SamplingIntent intent;

    intent = sampler.getSamplingIntent(null, "span name", SpanKind.CONSUMER, null, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());

    intent = sampler.getSamplingIntent(null, "span name", SpanKind.CLIENT, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0);
  }
}
