/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsistentAnyOfTest {

  static class TestSampler implements Composable {
    private final long threshold;
    private final boolean isAdjustedCountCorrect;

    public TestSampler(long threshold, boolean isAdjustedCountCorrect) {
      this.threshold = threshold;
      this.isAdjustedCountCorrect = isAdjustedCountCorrect;
    }

    @Override
    public SamplingIntent getSamplingIntent(
        Context parentContext,
        String name,
        SpanKind spanKind,
        Attributes attributes,
        List<LinkData> parentLinks) {

      return new SamplingIntent() {
        @Override
        public long getThreshold() {
          return threshold;
        }

        @Override
        public boolean isAdjustedCountReliable() {
          return isAdjustedCountCorrect;
        }
      };
    }

    @Override
    public String getDescription() {
      return "MockSampler for tests";
    }
  }

  @Test
  void testMinimumThresholdWithAdjustedCount() {
    Composable delegate1 = new TestSampler(0x80000000000000L, /* isAdjustedCountCorrect= */ false);
    Composable delegate2 = new TestSampler(0x30000000000000L, /* isAdjustedCountCorrect= */ true);
    Composable delegate3 = new TestSampler(0xa0000000000000L, /* isAdjustedCountCorrect= */ false);
    Composable delegate4 = new TestSampler(0x30000000000000L, /* isAdjustedCountCorrect= */ false);

    Composable sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3, delegate4);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isAdjustedCountReliable()).isTrue();

    // Change the delegate order
    sampler = ConsistentSampler.anyOf(delegate1, delegate4, delegate3, delegate2);
    intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isAdjustedCountReliable()).isTrue();
  }

  @Test
  void testMinimumThresholdWithoutAdjustedCount() {
    Composable delegate1 = new TestSampler(0x80000000000000L, /* isAdjustedCountCorrect= */ true);
    Composable delegate2 = new TestSampler(0x30000000000000L, /* isAdjustedCountCorrect= */ false);
    Composable delegate3 = new TestSampler(0xa0000000000000L, /* isAdjustedCountCorrect= */ true);

    Composable sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isAdjustedCountReliable()).isFalse();
  }

  @Test
  void testAlwaysDrop() {
    Composable delegate1 = ConsistentSampler.alwaysOff();
    Composable sampler = ConsistentSampler.anyOf(delegate1);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
    assertThat(intent.isAdjustedCountReliable()).isFalse();
  }

  @Test
  void testSpanAttributesAdded() {
    AttributeKey<String> key1 = AttributeKey.stringKey("tag1");
    AttributeKey<String> key2 = AttributeKey.stringKey("tag2");
    AttributeKey<String> key3 = AttributeKey.stringKey("tag3");
    Composable delegate1 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x30000000000000L), key1, "a");
    Composable delegate2 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x50000000000000L), key2, "b");
    Composable delegate3 = new MarkingSampler(ConsistentSampler.alwaysOff(), key3, "c");
    Composable sampler = ConsistentSampler.anyOf(delegate1, delegate2, delegate3);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("a");
    assertThat(intent.getAttributes().get(key2)).isEqualTo("b");
    assertThat(intent.getAttributes().get(key3)).isEqualTo("c");
    assertThat(intent.getThreshold()).isEqualTo(0x30000000000000L);
    assertThat(intent.isAdjustedCountReliable()).isTrue();
  }

  @Test
  void testSpanAttributeOverride() {
    AttributeKey<String> key1 = AttributeKey.stringKey("shared");
    Composable delegate1 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x30000000000000L), key1, "a");
    Composable delegate2 =
        new MarkingSampler(new ConsistentFixedThresholdSampler(0x50000000000000L), key1, "b");
    Composable sampler = ConsistentSampler.anyOf(delegate1, delegate2);
    SamplingIntent intent = sampler.getSamplingIntent(null, "span_name", null, null, null);
    assertThat(intent.getAttributes().get(key1)).isEqualTo("b");
  }
}
