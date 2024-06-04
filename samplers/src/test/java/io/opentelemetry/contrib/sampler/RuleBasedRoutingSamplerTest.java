/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;
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

    patterns.add(new SamplingRule(URL_FULL, ".*/healthcheck", Sampler.alwaysOff()));
    patterns.add(new SamplingRule(URL_PATH, "/actuator", Sampler.alwaysOff()));
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
            () -> RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).drop(URL_FULL, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> RuleBasedRoutingSampler.builder(SPAN_KIND, delegate).recordAndSample(null, ""));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () ->
                RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)
                    .recordAndSample(URL_FULL, null));
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
    attributes = Attributes.of(URL_FULL, "https://example.com");
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
    Attributes attributes = Attributes.of(URL_PATH, "/actuator/info");
    assertThat(
            sampler
                .shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void customSampler() {
    Attributes attributes = Attributes.of(URL_PATH, "/test");
    RuleBasedRoutingSampler testSampler =
        RuleBasedRoutingSampler.builder(SPAN_KIND, delegate)
            .customize(URL_PATH, ".*test", new AlternatingSampler())
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

  @Test
  void testThreadNameSampler() {
    patterns.add(new SamplingRule(THREAD_NAME, "Test.*", Sampler.alwaysOff()));
    Attributes attributes = Attributes.of(THREAD_NAME, "Test worker");
    RuleBasedRoutingSampler sampler = new RuleBasedRoutingSampler(patterns, SPAN_KIND, delegate);
    SamplingResult samplingResult =
        sampler.shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
    assertThat(samplingResult.getDecision()).isEqualTo(SamplingDecision.DROP);
  }

  private SamplingResult shouldSample(Sampler sampler, String url) {
    Attributes attributes = Attributes.of(URL_FULL, url);
    return sampler.shouldSample(
        parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
  }

  private static RuleBasedRoutingSamplerBuilder addRules(RuleBasedRoutingSamplerBuilder builder) {
    return builder.drop(URL_FULL, ".*/healthcheck").drop(URL_PATH, "/actuator");
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
