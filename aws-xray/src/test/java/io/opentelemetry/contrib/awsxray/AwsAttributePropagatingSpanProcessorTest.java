/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AwsAttributePropagatingSpanProcessorTest {
  private Tracer tracer;

  @BeforeEach
  public void setup() {
    tracer =
        SdkTracerProvider.builder()
            .addSpanProcessor(AwsAttributePropagatingSpanProcessor.create())
            .build()
            .get("awsxray");
  }

  @Test
  public void testRemoteAttributesInheritance() {
    Span spanWithAppOnly = tracer.spanBuilder("parent").startSpan();
    spanWithAppOnly.setAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION, "testApplication");
    validateSpanAttributesInheritance(spanWithAppOnly, null, "testApplication", null);

    Span spanWithOpOnly = tracer.spanBuilder("parent").startSpan();
    spanWithOpOnly.setAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION, "testOperation");
    validateSpanAttributesInheritance(spanWithOpOnly, null, null, "testOperation");

    Span spanWithAppAndOp = tracer.spanBuilder("parent").startSpan();
    spanWithAppAndOp.setAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION, "testApplication");
    spanWithAppAndOp.setAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION, "testOperation");
    validateSpanAttributesInheritance(spanWithAppAndOp, null, "testApplication", "testOperation");
  }

  @Test
  public void testOverrideRemoteAttributes() {
    Span parentSpan = tracer.spanBuilder("parent").startSpan();
    parentSpan.setAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION, "testApplication");
    parentSpan.setAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION, "testOperation");

    Span transmitSpans1 = createNestedSpan(parentSpan, 2);

    Span childSpan =
        tracer.spanBuilder("child:1").setParent(Context.current().with(transmitSpans1)).startSpan();

    childSpan.setAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION, "childOperation");

    Span transmitSpans2 = createNestedSpan(childSpan, 2);

    assertThat(((ReadableSpan) transmitSpans2).getAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION))
        .isEqualTo("childOperation");
  }

  @Test
  public void testRemoteAttributesNotExists() {
    Span span = tracer.spanBuilder("parent").startSpan();
    validateSpanAttributesInheritance(span, null, null, null);
  }

  @Test
  public void testLocalAttributeDetectionBySpanKind() {
    for (SpanKind value : SpanKind.values()) {
      Span span = tracer.spanBuilder("startOperation").setSpanKind(value).startSpan();
      if (value == SpanKind.SERVER || value == SpanKind.CONSUMER) {
        validateSpanAttributesInheritance(span, "startOperation", null, null);
      } else {
        validateSpanAttributesInheritance(span, null, null, null);
      }
    }
  }

  @Test
  public void testLocalAttributeDetectionWithRemoteParentSpan() {
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
            .spanBuilder("startOperation")
            .setSpanKind(SpanKind.SERVER)
            .setParent(parentcontext)
            .startSpan();
    validateSpanAttributesInheritance(span, "startOperation", null, null);
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
      Span spanWithAppOnly,
      String localOperation,
      String remoteApplication,
      String remoteOperation) {
    ReadableSpan leafSpan = (ReadableSpan) createNestedSpan(spanWithAppOnly, 10);

    assertThat(leafSpan.getParentSpanContext()).isNotNull();
    assertThat(leafSpan.getName()).isEqualTo("child:1");
    if (localOperation != null) {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_LOCAL_OPERATION))
          .isEqualTo(localOperation);
    } else {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_LOCAL_OPERATION)).isNull();
    }
    if (remoteApplication != null) {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION))
          .isEqualTo(remoteApplication);
    } else {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION)).isNull();
    }
    if (remoteOperation != null) {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION))
          .isEqualTo(remoteOperation);
    } else {
      assertThat(leafSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION)).isNull();
    }
  }
}
