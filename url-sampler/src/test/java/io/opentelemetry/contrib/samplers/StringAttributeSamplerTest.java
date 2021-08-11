/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringAttributeSamplerTest {
  private static final String SPAN_NAME = "MySpanName";
  private static final SpanKind SPAN_KIND = SpanKind.SERVER;
  private final IdGenerator idsGenerator = IdGenerator.random();
  private final String traceId = idsGenerator.generateTraceId();
  private final String parentSpanId = idsGenerator.generateSpanId();
  private final SpanContext sampledSpanContext =
      SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault());
  private final Context parentContext = Context.root().with(Span.wrap(sampledSpanContext));

  private final Map<AttributeKey<String>, Collection<String>> patterns = new HashMap<>();

  @Mock(lenient = true)
  private Sampler delegate;

  @BeforeEach
  public void setup() {
    when(delegate.shouldSample(any(), any(), any(), any(), any(), any()))
        .thenReturn(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));

    patterns.put(HTTP_URL, singletonList(".*/healthcheck"));
    patterns.put(HTTP_TARGET, singletonList("/actuator"));
  }

  @Test
  public void testThatThrowsOnNullParameter() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new StringAttributeSampler(patterns, SPAN_KIND, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new StringAttributeSampler(null, SPAN_KIND, delegate));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new StringAttributeSampler(patterns, null, delegate));

  }

  @Test
  public void testThatDelegatesIfNoPatternsGiven() {
    StringAttributeSampler sampler = new StringAttributeSampler(emptyMap(), SPAN_KIND, delegate);

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
    StringAttributeSampler sampler = new StringAttributeSampler(patterns, SPAN_KIND, delegate);
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  public void testDelegateOnDifferentKind() {
    StringAttributeSampler sampler = new StringAttributeSampler(patterns, SpanKind.CLIENT, delegate);
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDelegateOnNoMatch() {
    StringAttributeSampler sampler = new StringAttributeSampler(patterns, SPAN_KIND, delegate);
    assertThat(shouldSample(sampler, "https://example.com/customers").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDelegateOnMalformedUrl() {
    StringAttributeSampler sampler = new StringAttributeSampler(patterns, SPAN_KIND, delegate);
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
    StringAttributeSampler sampler = new StringAttributeSampler(patterns, SPAN_KIND, delegate);
    Attributes attributes = Attributes.of(HTTP_TARGET, "/actuator/info");
    assertThat(sampler.shouldSample(parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList()).getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  private SamplingResult shouldSample(Sampler sampler, String url) {
    Attributes attributes = Attributes.of(HTTP_URL, url);
    return sampler.shouldSample(
        parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
  }
}
