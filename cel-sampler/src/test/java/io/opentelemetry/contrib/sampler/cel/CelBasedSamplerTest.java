/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_HEADER;
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

import dev.cel.common.CelValidationException;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class CelBasedSamplerTest {

  private final IdGenerator idsGenerator = IdGenerator.random();
  private final String traceId = idsGenerator.generateTraceId();
  private final String parentSpanId = idsGenerator.generateSpanId();
  private final SpanContext sampledSpanContext =
      SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault());
  private final Context parentContext = Context.root().with(Span.wrap(sampledSpanContext));
  private final List<CelBasedSamplingExpression> expressions = new ArrayList<>();

  @Mock(strictness = Mock.Strictness.LENIENT)
  private Sampler delegate;

  @BeforeEach
  void setup() throws CelValidationException {
    when(delegate.shouldSample(any(), any(), any(), any(), any(), any()))
        .thenReturn(SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE));

    expressions.add(
        new CelBasedSamplingExpression(
            CelBasedSampler.celCompiler
                .compile(
                    "spanKind == 'SERVER' && attribute[\""
                        + URL_FULL.getKey()
                        + "\"].matches(\"/actuator\")")
                .getAst(),
            Sampler.alwaysOff()));
    expressions.add(
        new CelBasedSamplingExpression(
            CelBasedSampler.celCompiler
                .compile(
                    "spanKind == 'SERVER' && attribute[\""
                        + URL_FULL.getKey()
                        + "\"].matches(\".*/healthcheck\")")
                .getAst(),
            Sampler.alwaysOff()));
  }

  @Test
  void testThatThrowsOnNullParameter() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSampler(expressions, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSampler(null, delegate));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> CelBasedSampler.builder(null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> CelBasedSampler.builder(delegate).drop(null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> CelBasedSampler.builder(delegate).recordAndSample(null));
  }

  @Test
  void testThatDelegatesIfNoExpressionGiven() {
    CelBasedSampler sampler = CelBasedSampler.builder(delegate).build();

    // no http.url attribute
    Attributes attributes = Attributes.empty();
    sampler.shouldSample(
        parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
    verify(delegate)
        .shouldSample(
            parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());

    clearInvocations(delegate);

    // with http.url attribute
    attributes = Attributes.of(URL_FULL, "https://example.com");
    sampler.shouldSample(
        parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
    verify(delegate)
        .shouldSample(
            parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
  }

  @Test
  void testDropOnExactMatch() throws CelValidationException {
    CelBasedSampler sampler = addRules(CelBasedSampler.builder(delegate)).build();
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void testDelegateOnDifferentKind() throws CelValidationException {
    CelBasedSampler sampler =
        addRules(CelBasedSampler.builder(delegate), SpanKind.CLIENT.name()).build();
    assertThat(shouldSample(sampler, "https://example.com/healthcheck").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testDelegateOnNoMatch() throws CelValidationException {
    CelBasedSampler sampler = addRules(CelBasedSampler.builder(delegate)).build();
    assertThat(shouldSample(sampler, "https://example.com/customers").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testDelegateOnMalformedUrl() throws CelValidationException {
    CelBasedSampler sampler = addRules(CelBasedSampler.builder(delegate)).build();
    assertThat(shouldSample(sampler, "abracadabra").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());

    clearInvocations(delegate);

    assertThat(shouldSample(sampler, "healthcheck").getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    verify(delegate).shouldSample(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testVerifiesAllGivenAttributes() throws CelValidationException {
    CelBasedSampler sampler = addRules(CelBasedSampler.builder(delegate)).build();
    Attributes attributes = Attributes.of(URL_PATH, "/actuator/info");
    assertThat(
            sampler
                .shouldSample(
                    parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void customSampler() throws CelValidationException {
    Attributes attributes = Attributes.of(URL_PATH, "/test");
    CelBasedSampler testSampler =
        CelBasedSampler.builder(delegate)
            .customize(
                "attribute[\"" + URL_PATH + "\"].matches(\".*test\")", new AlternatingSampler())
            .build();
    assertThat(
            testSampler
                .shouldSample(
                    parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.DROP);
    assertThat(
            testSampler
                .shouldSample(
                    parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList())
                .getDecision())
        .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void testThreadNameSampler() throws CelValidationException {
    expressions.add(
        new CelBasedSamplingExpression(
            CelBasedSampler.celCompiler
                .compile(
                    "spanKind == 'SERVER' && attribute[\""
                        + THREAD_NAME
                        + "\"].matches(\"Test.*\")")
                .getAst(),
            Sampler.alwaysOff()));
    Attributes attributes = Attributes.of(THREAD_NAME, "Test worker");
    CelBasedSampler sampler = new CelBasedSampler(expressions, delegate);
    SamplingResult samplingResult =
        sampler.shouldSample(
            parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
    assertThat(samplingResult.getDecision()).isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void testComplexAttributeSampler() throws CelValidationException {
    expressions.add(
        new CelBasedSamplingExpression(
            CelBasedSampler.celCompiler
                .compile(
                    "\"example.com\" in attribute[\""
                        + HTTP_RESPONSE_HEADER.getAttributeKey("host")
                        + "\"]")
                .getAst(),
            Sampler.alwaysOff()));
    Attributes attributes =
        Attributes.of(
            HTTP_RESPONSE_HEADER.getAttributeKey("host"),
            Arrays.asList("example.com", "example.org"));
    CelBasedSampler sampler = new CelBasedSampler(expressions, delegate);
    SamplingResult samplingResult =
        sampler.shouldSample(
            parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
    assertThat(samplingResult.getDecision()).isEqualTo(SamplingDecision.DROP);
  }

  private SamplingResult shouldSample(Sampler sampler, String url) {
    Attributes attributes = Attributes.of(URL_FULL, url);
    return sampler.shouldSample(
        parentContext, traceId, "MySpanName", SpanKind.SERVER, attributes, emptyList());
  }

  private static CelBasedSamplerBuilder addRules(CelBasedSamplerBuilder builder, String kind)
      throws CelValidationException {
    return builder
        .drop(
            "attribute[\""
                + URL_FULL.getKey()
                + "\"].matches(\".*/healthcheck\") && spanKind == '"
                + kind
                + "'")
        .drop(
            "attribute[\"" + URL_PATH + "\"].matches(\"/actuator\") && spanKind == '" + kind + "'");
  }

  private static CelBasedSamplerBuilder addRules(CelBasedSamplerBuilder builder)
      throws CelValidationException {
    return addRules(builder, SpanKind.SERVER.name());
  }

  /** Silly sampler that alternates decisions for testing. */
  private static final class AlternatingSampler implements Sampler {
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
