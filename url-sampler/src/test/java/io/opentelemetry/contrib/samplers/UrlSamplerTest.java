/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlSamplerTest {
  private static final String SPAN_NAME = "MySpanName";
  private static final SpanKind SPAN_KIND = SpanKind.INTERNAL;
  private final IdGenerator idsGenerator = IdGenerator.random();
  private final String traceId = idsGenerator.generateTraceId();
  private final String parentSpanId = idsGenerator.generateSpanId();
  private final SpanContext sampledSpanContext =
      SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault());
  private final Context parentContext = Context.root().with(Span.wrap(sampledSpanContext));

  private final Collection<String> patterns = singletonList("/healthcheck");

  @Mock(lenient = true)
  private Sampler delegate;

  @BeforeEach
  public void setup() {
    when(delegate.shouldSample(any(), any(), any(), any(), any(), any()))
        .thenReturn(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));
  }

  @Test
  public void testThatThrowsOnNullDelegateOrPatterns() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new UrlSampler(patterns, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new UrlSampler(null, delegate));
  }

  @Test
  public void testThatDelegatesIfNoPatternsGiven() {
    UrlSampler sampler = new UrlSampler(emptyList(), delegate);

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
    UrlSampler sampler = new UrlSampler(patterns, delegate);
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  public void testDelegateOnNoMatch() {
    UrlSampler sampler = new UrlSampler(patterns, delegate);
    assertThat(shouldSample(sampler, "https://example.com/customers").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDelegateOnMalformedUrl() {
    UrlSampler sampler = new UrlSampler(patterns, delegate);
    assertThat(shouldSample(sampler, "abracadabra").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  private SamplingResult shouldSample(Sampler sampler, String url) {
    Attributes attributes = Attributes.of(HTTP_URL, url);
    return sampler.shouldSample(
        parentContext, traceId, SPAN_NAME, SPAN_KIND, attributes, emptyList());
  }
}
