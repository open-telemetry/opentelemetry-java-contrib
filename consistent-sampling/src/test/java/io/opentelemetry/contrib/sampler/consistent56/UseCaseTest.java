/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSampler.alwaysOff;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSampler.alwaysOn;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.Predicate.anySpan;
import static io.opentelemetry.contrib.sampler.consistent56.Predicate.isRootSpan;
import static io.opentelemetry.contrib.sampler.consistent56.PredicatedSampler.onMatch;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.jupiter.api.Test;

/**
 * Testing a "real life" sampler configuration, as provided as an example in
 * https://github.com/open-telemetry/oteps/pull/250. The example uses many different composite
 * samplers combining them together to demonstrate the expressiveness and flexibility of the
 * proposed specification.
 */
class UseCaseTest {
  private static final long[] nanoTime = new long[] {0L};

  private static final long nanoTime() {
    return nanoTime[0];
  }

  private static void advanceTime(long nanosIncrement) {
    nanoTime[0] += nanosIncrement;
  }

  //
  //     S = ConsistentRateLimiting(
  //      ConsistentAnyOf(
  //        ConsistentParentBased(
  //          ConsistentRuleBased(ROOT, {
  //              (http.target == /healthcheck) => ConsistentAlwaysOff,
  //              (http.target == /checkout) => ConsistentAlwaysOn,
  //              true => ConsistentFixedThreshold(0.25)
  //          }),
  //        ConsistentRuleBased(CLIENT, {
  //          (http.url == /foo) => ConsistentAlwaysOn
  //        }
  //      ),
  //      1000.0
  //    )
  //
  private static final AttributeKey<String> httpTarget = AttributeKey.stringKey("http.target");
  private static final AttributeKey<String> httpUrl = AttributeKey.stringKey("http.url");

  private static ConsistentSampler buildSampler() {
    Predicate healthCheck =
        Predicate.and(
            isRootSpan(),
            (parentContext, name, spanKind, attributes, parentLinks) -> {
              return "/healthCheck".equals(attributes.get(httpTarget));
            });
    Predicate checkout =
        Predicate.and(
            isRootSpan(),
            (parentContext, name, spanKind, attributes, parentLinks) -> {
              return "/checkout".equals(attributes.get(httpTarget));
            });
    ComposableSampler s1 =
        ConsistentSampler.parentBased(
            ConsistentSampler.ruleBased(
                null,
                onMatch(healthCheck, alwaysOff()),
                onMatch(checkout, alwaysOn()),
                onMatch(anySpan(), ConsistentSampler.probabilityBased(0.25))));
    Predicate foo =
        (parentContext, name, spanKind, attributes, parentLinks) -> {
          return "/foo".equals(attributes.get(httpUrl));
        };

    ComposableSampler s2 = ConsistentSampler.ruleBased(SpanKind.CLIENT, onMatch(foo, alwaysOn()));
    ComposableSampler s3 = ConsistentSampler.anyOf(s1, s2);
    return ConsistentSampler.rateLimited(s3, 1000.0, 5, UseCaseTest::nanoTime);
  }

  @Test
  void testDropHealthcheck() {
    ConsistentSampler s = buildSampler();
    Attributes attributes = createAttributes(httpTarget, "/healthCheck");
    SamplingIntent intent = s.getSamplingIntent(null, "A", SpanKind.SERVER, attributes, null);
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
  }

  @Test
  void testSampleCheckout() {
    ConsistentSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = createAttributes(httpTarget, "/checkout");
    SamplingIntent intent = s.getSamplingIntent(null, "B", SpanKind.SERVER, attributes, null);
    assertThat(intent.getThreshold()).isEqualTo(0L);
    advanceTime(1000); // rate limiting should kick in
    intent = s.getSamplingIntent(null, "B", SpanKind.SERVER, attributes, null);
    assertThat(intent.getThreshold()).isGreaterThan(0L);
  }

  @Test
  void testSampleClient() {
    ConsistentSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = createAttributes(httpUrl, "/foo");
    SamplingIntent intent = s.getSamplingIntent(null, "C", SpanKind.CLIENT, attributes, null);
    assertThat(intent.getThreshold()).isEqualTo(0L);
  }

  @Test
  void testOtherRoot() {
    ConsistentSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = Attributes.empty();
    SamplingIntent intent = s.getSamplingIntent(null, "D", SpanKind.SERVER, attributes, null);
    assertThat(intent.getThreshold()).isEqualTo(0xc0000000000000L);
  }

  private static Attributes createAttributes(AttributeKey<String> key, String value) {
    return Attributes.builder().put(key, value).build();
  }
}
