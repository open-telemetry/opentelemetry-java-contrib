/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributePropagatingSpanProcessorTest {

  private Tracer tracer;

  AttributeKey<String> spanNameKey = AttributeKey.stringKey("spanName");
  AttributeKey<String> testKey1 = AttributeKey.stringKey("key1");
  AttributeKey<String> testKey2 = AttributeKey.stringKey("key2");

  @BeforeEach
  public void setup() {
    tracer =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                AttributePropagatingSpanProcessor.create(
                    spanNameKey, Arrays.asList(testKey1, testKey2)))
            .build()
            .get("awsxray");
  }

  @Test
  public void testAttributesPropagation() {
    Span spanWithAppOnly = tracer.spanBuilder("parent").startSpan();
    spanWithAppOnly.setAttribute(testKey1, "testValue1");
    validateSpanAttributesInheritance(spanWithAppOnly, null, "testValue1", null);

    Span spanWithOpOnly = tracer.spanBuilder("parent").startSpan();
    spanWithOpOnly.setAttribute(testKey2, "testValue2");
    validateSpanAttributesInheritance(spanWithOpOnly, null, null, "testValue2");

    Span spanWithAppAndOp = tracer.spanBuilder("parent").startSpan();
    spanWithAppAndOp.setAttribute(testKey1, "testValue1");
    spanWithAppAndOp.setAttribute(testKey2, "testValue2");
    validateSpanAttributesInheritance(spanWithAppAndOp, null, "testValue1", "testValue2");
  }

  @Test
  public void testOverrideAttributes() {
    Span parentSpan = tracer.spanBuilder("parent").startSpan();
    parentSpan.setAttribute(testKey1, "testValue1");
    parentSpan.setAttribute(testKey2, "testValue2");

    Span transmitSpans1 = createNestedSpan(parentSpan, 2);

    Span childSpan =
        tracer.spanBuilder("child:1").setParent(Context.current().with(transmitSpans1)).startSpan();

    childSpan.setAttribute(testKey2, "testValue3");

    Span transmitSpans2 = createNestedSpan(childSpan, 2);

    assertThat(((ReadableSpan) transmitSpans2).getAttribute(testKey2)).isEqualTo("testValue3");
  }

  @Test
  public void testAttributesDoNotExist() {
    Span span = tracer.spanBuilder("parent").startSpan();
    validateSpanAttributesInheritance(span, null, null, null);
  }

  @Test
  public void testSpanNamePropagationBySpanKind() {
    for (SpanKind value : SpanKind.values()) {
      Span span = tracer.spanBuilder("parent").setSpanKind(value).startSpan();
      if (value == SpanKind.SERVER || value == SpanKind.CONSUMER) {
        validateSpanAttributesInheritance(span, "parent", null, null);
      } else {
        validateSpanAttributesInheritance(span, null, null, null);
      }
    }
  }

  @Test
  public void testSpanNamePropagationWithRemoteParentSpan() {
    Span remoteParent =
        Span.wrap(
            SpanContext.createFromRemoteParent(
                "00000000000000000000000000000001",
                "0000000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
    Context parentcontext = Context.root().with(remoteParent);
    Span span =
        tracer
            .spanBuilder("parent")
            .setSpanKind(SpanKind.SERVER)
            .setParent(parentcontext)
            .startSpan();
    validateSpanAttributesInheritance(span, "parent", null, null);
  }

  private Span createNestedSpan(Span parentSpan, int depth) {
    if (depth == 0) {
      return parentSpan;
    }
    Span childSpan =
        tracer
            .spanBuilder("child:" + depth)
            .setParent(Context.current().with(parentSpan))
            .startSpan();
    try {
      return createNestedSpan(childSpan, depth - 1);
    } finally {
      childSpan.end();
    }
  }

  private void validateSpanAttributesInheritance(
      Span parentSpan, String propagatedName, String propagationValue1, String propagatedValue2) {
    ReadableSpan leafSpan = (ReadableSpan) createNestedSpan(parentSpan, 10);

    assertThat(leafSpan.getParentSpanContext()).isNotNull();
    assertThat(leafSpan.getName()).isEqualTo("child:1");
    if (propagatedName != null) {
      assertThat(leafSpan.getAttribute(spanNameKey)).isEqualTo(propagatedName);
    } else {
      assertThat(leafSpan.getAttribute(spanNameKey)).isNull();
    }
    if (propagationValue1 != null) {
      assertThat(leafSpan.getAttribute(testKey1)).isEqualTo(propagationValue1);
    } else {
      assertThat(leafSpan.getAttribute(testKey1)).isNull();
    }
    if (propagatedValue2 != null) {
      assertThat(leafSpan.getAttribute(testKey2)).isEqualTo(propagatedValue2);
    } else {
      assertThat(leafSpan.getAttribute(testKey2)).isNull();
    }
  }
}
