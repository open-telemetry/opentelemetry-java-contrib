/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import org.junit.jupiter.api.Test;

class ConsistentAnyOfTest {

  @Test
  void testMinimumThreshold() {
    ComposableSampler delegate1 = new ConsistentFixedThresholdSampler(0x80000000000000L);
    ComposableSampler delegate2 = new ConsistentFixedThresholdSampler(0x30000000000000L);
    ComposableSampler delegate3 = new ConsistentFixedThresholdSampler(0xa0000000000000L);
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
  }

  @Test
  void testAlwaysDrop() {
    ComposableSampler delegate1 = ConsistentSampler.alwaysOff();
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
  }

  @Test
  void testSpanAttributesAdded() {
    AttributeKey<String> key1 = AttributeKey.stringKey("tag1");
    AttributeKey<String> key2 = AttributeKey.stringKey("tag2");
    AttributeKey<String> key3 = AttributeKey.stringKey("tag3");
    ComposableSampler delegate1 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x30000000000000L), key1, "a");
    ComposableSampler delegate2 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x50000000000000L), key2, "b");
    ComposableSampler delegate3 = new MarkingSampler(ConsistentSampler.alwaysOff(), key3, "c");
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("a");
    assertThat(intent.getAttributes().get(key2)).isEqualTo("b");
    assertThat(intent.getAttributes().get(key3)).isEqualTo("c");
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
  }

  @Test
  void testSpanAttributeOverride() {
    AttributeKey<String> key1 = AttributeKey.stringKey("shared");
    ComposableSampler delegate1 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x30000000000000L), key1, "a");
    ComposableSampler delegate2 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x50000000000000L), key1, "b");
    ComposableSampler sampler = ConsistentSampler.anyOf(delegate1, delegate2);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("b");
  }
}
