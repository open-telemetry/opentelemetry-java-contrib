/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.getInvalidThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingPredicate;
import java.util.Collections;
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
  //        parentThreshold(
  //          ruleBased(ROOT, {
  //              (http.target == /healthcheck) => alwaysOff,
  //              (http.target == /checkout) => alwaysOn,
  //              true => probability(0.25)
  //          }),
  //        ruleBased(CLIENT, {
  //          (http.url == /foo) => alwaysOn
  //        }
  //      ),
  //      1000.0
  //    )
  //
  private static final AttributeKey<String> httpTarget = AttributeKey.stringKey("http.target");
  private static final AttributeKey<String> httpUrl = AttributeKey.stringKey("http.url");

  private static ComposableSampler buildSampler() {
    SamplingPredicate healthCheck =
        (parentContext, traceId, name, spanKind, attributes, parentLinks) -> {
          return isRootSpan(parentContext) && "/healthCheck".equals(attributes.get(httpTarget));
        };
    SamplingPredicate checkout =
        (parentContext, traceId, name, spanKind, attributes, parentLinks) -> {
          return isRootSpan(parentContext) && "/checkout".equals(attributes.get(httpTarget));
        };

    ComposableSampler s1 =
        ComposableSampler.parentThreshold(
            ComposableSampler.ruleBasedBuilder()
                .add(healthCheck, ComposableSampler.alwaysOff())
                .add(checkout, ComposableSampler.alwaysOn())
                .add(
                    (parentContext, traceId, name, spanKind, attributes, parentLinks) -> true,
                    ComposableSampler.probability(0.25))
                .build());

    SamplingPredicate foo =
        (parentContext, traceId, name, spanKind, attributes, parentLinks) -> {
          return spanKind == SpanKind.CLIENT && "/foo".equals(attributes.get(httpUrl));
        };

    ComposableSampler s2 =
        ComposableSampler.ruleBasedBuilder().add(foo, ComposableSampler.alwaysOn()).build();
    ComposableSampler s3 = ConsistentSampler.anyOf(s1, s2);
    return ConsistentSampler.rateLimited(s3, 1000.0, 5, UseCaseTest::nanoTime);
  }

  @Test
  void testDropHealthcheck() {
    ComposableSampler s = buildSampler();
    Attributes attributes = createAttributes(httpTarget, "/healthCheck");
    SamplingIntent intent =
        s.getSamplingIntent(
            Context.root(), "A", "span_name", SpanKind.SERVER, attributes, Collections.emptyList());
    assertThat(intent.getThreshold()).isEqualTo(getInvalidThreshold());
  }

  @Test
  void testSampleCheckout() {
    ComposableSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = createAttributes(httpTarget, "/checkout");
    SamplingIntent intent =
        s.getSamplingIntent(
            Context.root(), "B", "span_name", SpanKind.SERVER, attributes, Collections.emptyList());
    assertThat(intent.getThreshold()).isEqualTo(0L);
    advanceTime(1000); // rate limiting should kick in
    intent =
        s.getSamplingIntent(
            Context.root(), "B", "span_name", SpanKind.SERVER, attributes, Collections.emptyList());
    assertThat(intent.getThreshold()).isGreaterThan(0L);
  }

  @Test
  void testSampleClient() {
    ComposableSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = createAttributes(httpUrl, "/foo");
    SamplingIntent intent =
        s.getSamplingIntent(
            Context.root(), "C", "span_name", SpanKind.CLIENT, attributes, Collections.emptyList());
    assertThat(intent.getThreshold()).isEqualTo(0L);
  }

  @Test
  void testOtherRoot() {
    ComposableSampler s = buildSampler();
    advanceTime(1000000);
    Attributes attributes = Attributes.empty();
    SamplingIntent intent =
        s.getSamplingIntent(
            Context.root(), "D", "span_name", SpanKind.SERVER, attributes, Collections.emptyList());
    assertThat(intent.getThreshold()).isEqualTo(0xc0000000000000L);
  }

  private static boolean isRootSpan(Context parentContext) {
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    return !parentSpanContext.isValid();
  }

  private static Attributes createAttributes(AttributeKey<String> key, String value) {
    return Attributes.builder().put(key, value).build();
  }
}
