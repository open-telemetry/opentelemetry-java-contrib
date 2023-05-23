/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleBasedRoutingSamplerTest {
  private static final String SPAN_NAME = "MySpanName";
  private static final SpanKind SPAN_KIND = SpanKind.SERVER;
  private final IdGenerator idsGenerator = IdGenerator.random();
  private final String traceId = idsGenerator.generateTraceId();
  private final String parentSpanId = idsGenerator.generateSpanId();
  private final SpanContext sampledSpanContext =
      SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault());
  private final Context parentContext = Context.root().with(Span.wrap(sampledSpanContext));

  private final List<SamplingRule> patterns = new ArrayList<>();

  @Mock(strictness = Mock.Strictness.LENIENT)
  private Sampler delegate;

  @BeforeEach
  public void setup() {
    when(delegate.shouldSample(any(), any(), any(), any(), any(), any()))
        .thenReturn(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));

    patterns.add(new SamplingRule(HTTP_URL, ".*/healthcheck", Sampler.alwaysOff()));
    patterns.add(new SamplingRule(HTTP_TARGET, "/actuator", Sampler.alwaysOff()));
  }

  @Test
  public void testThatThrowsOnNullParameter() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new RuleBasedRoutingSampler(patterns, SPAN_KIND, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new RuleBasedRoutingSampler(null, SPAN_KIND, delegate));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new RuleBasedRoutingSampler(patterns, null, delegate));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> RuleBasedRoutingSampler.builder(SPAN_KIND, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> RuleBasedRoutingSampler.builder(null, delegate));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).drop(null, ""));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).drop(HTTP_URL, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).recordAndSample(null, ""));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () ->
                RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)
                    .recordAndSample(HTTP_URL, null));
  }

  @Test
  public void testThatDelegatesIfNoRulesGiven() {
    RuleBasedRoutingSampler sampler = RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).build();

    // no http.url attribute
    Attributes attributes = Attributes.empty();
    sampler.shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
    verify(delegate)
        .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());

    clearInvocations(delegate);

    // with http.url attribute
    attributes = Attributes.of(HTTP_URL, "https://example.com");
    sampler.shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
    verify(delegate)
        .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
  }

  @Test
  public void testDropOnExactMatch() {
    RuleBasedRoutingSampler sampler =
        addRules(RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)).build();
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  public void testDelegateOnDifferentKind() {
    RuleBasedRoutingSampler sampler =
        addRules(RuleBasedRoutingSampler.builder(SpanKind.CLIENT, delegate)).build();
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDelegateOnNoMatch() {
    RuleBasedRoutingSampler sampler =
        addRules(RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)).build();
    assertThat(shouldSample(sampler, "https://example.com/customers").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDelegateOnMalformedUrl() {
    RuleBasedRoutingSampler sampler =
        addRules(RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)).build();
    assertThat(shouldSample(sampler, "abracadabra").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());

    clearInvocations(delegate);

    assertThat(shouldSample(sampler, "healthcheck").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testVerifiesAllGivenAttributes() {
    RuleBasedRoutingSampler sampler =
        addRules(RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)).build();
    Attributes attributes = Attributes.of(HTTP_TARGET, "/actuator/info");
    assertThat(
            sampler
                .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void customSampler() {
    Attributes attributes = Attributes.of(HTTP_TARGET, "/test");
    RuleBasedRoutingSampler testSampler =
        RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)
            .customize(HTTP_TARGET, ".*test", new AlternatingSampler())
            .build();
    assertThat(
            testSampler
                .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.DROP);
    assertThat(
            testSampler
                .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  private SamplingResult shouldSample(Sampler sampler, String url) {
    Attributes attributes = Attributes.of(HTTP_URL, url);
    return sampler.shouldSample(
        parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
  }

  private static RuleBasedRoutingSamplerBuilder addRules(RuleBasedRoutingSamplerBuilder builder) {
    return builder.drop(HTTP_URL, ".*/healthcheck").drop(HTTP_TARGET, "/actuator");
  }

  /** Silly sampler that alternates decisions for testing. */
  private static class AlternatingSampler implements Sampler {
    private final AtomicBoolean switcher = new AtomicBoolean();

    @Override
    public SamplingResult shouldSample(
        Context parentContext,
        String traceId,
        String name,
        SpanKind spanKind,
        Attributes attributes,
        List<LinkData> parentLinks) {
      return switcher.getAndSet(!switcher.get())
          ? SamplingResult.recordAndSample()
          : SamplingResult.drop();
    }

    @Override
    public String getDescription() {
      return "weird switching sampler for testing";
    }
  }
}
