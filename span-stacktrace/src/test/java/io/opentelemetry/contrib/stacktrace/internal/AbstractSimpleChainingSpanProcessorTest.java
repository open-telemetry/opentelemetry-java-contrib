/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace.internal;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractSimpleChainingSpanProcessorTest {

  private InMemorySpanExporter spans;
  private SpanProcessor exportProcessor;

  @BeforeEach
  public void setup() {
    spans = InMemorySpanExporter.create();
    exportProcessor = SimpleSpanProcessor.create(spans);
  }

  @Test
  public void testSpanDropping() {
    SpanProcessor processor =
        new AbstractSimpleChainingSpanProcessor(exportProcessor) {
          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            if (readableSpan.getName().startsWith("dropMe")) {
              return null;
            } else {
              return readableSpan;
            }
          }
        };
    try (OpenTelemetrySdk sdk = TestUtils.sdkWith(processor)) {
      Tracer tracer = sdk.getTracer("dummy-tracer");

      tracer.spanBuilder("dropMe1").startSpan().end();
      tracer.spanBuilder("sendMe").startSpan().end();
      tracer.spanBuilder("dropMe2").startSpan().end();

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(span -> assertThat(span).hasName("sendMe"));
    }
  }

  @Test
  public void testAttributeUpdate() {

    AttributeKey<String> keepMeKey = AttributeKey.stringKey("keepMe");
    AttributeKey<String> updateMeKey = AttributeKey.stringKey("updateMe");
    AttributeKey<String> addMeKey = AttributeKey.stringKey("addMe");
    AttributeKey<String> removeMeKey = AttributeKey.stringKey("removeMe");

    SpanProcessor second =
        new AbstractSimpleChainingSpanProcessor(exportProcessor) {
          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            MutableSpan span = MutableSpan.makeMutable(readableSpan);
            span.setAttribute(addMeKey, "added");
            return span;
          }
        };
    SpanProcessor first =
        new AbstractSimpleChainingSpanProcessor(second) {
          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            MutableSpan span = MutableSpan.makeMutable(readableSpan);
            span.setAttribute(updateMeKey, "updated");
            span.removeAttribute(removeMeKey);
            return span;
          }
        };
    try (OpenTelemetrySdk sdk = TestUtils.sdkWith(first)) {
      Tracer tracer = sdk.getTracer("dummy-tracer");

      tracer
          .spanBuilder("dropMe1")
          .startSpan()
          .setAttribute(keepMeKey, "keep-me-original")
          .setAttribute(removeMeKey, "remove-me-original")
          .setAttribute(updateMeKey, "foo")
          .end();

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(
              span -> {
                Attributes attribs = span.getAttributes();
                assertThat(attribs)
                    .hasSize(3)
                    .containsEntry(keepMeKey, "keep-me-original")
                    .containsEntry(updateMeKey, "updated")
                    .containsEntry(addMeKey, "added");
              });
    }
  }
}
