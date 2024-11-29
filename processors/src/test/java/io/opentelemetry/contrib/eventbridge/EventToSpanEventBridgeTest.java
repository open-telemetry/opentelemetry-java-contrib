/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.eventbridge;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.time.TestClock;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EventToSpanEventBridgeTest {

  private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
  private final SdkTracerProvider tracerProvider =
      SdkTracerProvider.builder()
          .setSampler(onlyServerSpans())
          .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
          .build();
  private final TestClock testClock = TestClock.create();
  private final SdkEventLoggerProvider eventLoggerProvider =
      SdkEventLoggerProvider.create(
          SdkLoggerProvider.builder()
              .setClock(testClock)
              .addLogRecordProcessor(EventToSpanEventBridge.create())
              .build());
  private final Tracer tracer = tracerProvider.get("tracer");
  private final EventLogger eventLogger = eventLoggerProvider.get("event-logger");

  private static Sampler onlyServerSpans() {
    return new Sampler() {
      @Override
      public SamplingResult shouldSample(
          Context parentContext,
          String traceId,
          String name,
          SpanKind spanKind,
          Attributes attributes,
          List<LinkData> parentLinks) {
        return SpanKind.SERVER.equals(spanKind)
            ? SamplingResult.recordAndSample()
            : SamplingResult.drop();
      }

      @Override
      public String getDescription() {
        return "description";
      }
    };
  }

  @Test
  void withRecordingSpan_BridgesEvent() {
    testClock.setTime(Instant.ofEpochMilli(1));

    // The test tracerProvider has a sampler which records and samples SERVER spans, and drops all
    // others. We create a recording span by setting kind to SERVER.
    Span span = tracer.spanBuilder("span").setSpanKind(SpanKind.SERVER).startSpan();
    try (Scope unused = span.makeCurrent()) {
      eventLogger
          .builder("my.event-name")
          .setTimestamp(100, TimeUnit.NANOSECONDS)
          .setSeverity(Severity.DEBUG)
          .put("foo", "bar")
          .put("number", 1)
          .put("map", Value.of(Collections.singletonMap("key", Value.of("value"))))
          .setAttributes(Attributes.builder().put("color", "red").build())
          .setAttributes(Attributes.builder().put("shape", "square").build())
          .emit();
    } finally {
      span.end();
    }

    assertThat(spanExporter.getFinishedSpanItems())
        .satisfiesExactly(
            spanData ->
                assertThat(spanData)
                    .hasName("span")
                    .hasEventsSatisfyingExactly(
                        spanEvent ->
                            spanEvent
                                .hasName("my.event-name")
                                .hasTimestamp(100, TimeUnit.NANOSECONDS)
                                .hasAttributesSatisfying(
                                    attributes -> {
                                      assertThat(attributes.get(stringKey("color")))
                                          .isEqualTo("red");
                                      assertThat(attributes.get(stringKey("shape")))
                                          .isEqualTo("square");
                                      assertThat(
                                              attributes.get(
                                                  longKey("log.record.observed_time_unix_nano")))
                                          .isEqualTo(1000000L);
                                      assertThat(
                                              attributes.get(longKey("log.record.severity_number")))
                                          .isEqualTo(Severity.DEBUG.getSeverityNumber());
                                      assertThat(attributes.get(stringKey("log.record.body")))
                                          .isEqualTo(
                                              "{\"kvlistValue\":{\"values\":[{\"key\":\"number\",\"value\":{\"intValue\":\"1\"}},{\"key\":\"foo\",\"value\":{\"stringValue\":\"bar\"}},{\"key\":\"map\",\"value\":{\"kvlistValue\":{\"values\":[{\"key\":\"key\",\"value\":{\"stringValue\":\"value\"}}]}}}]}}");
                                    })));
  }

  @Test
  void nonRecordingSpan_doesNotBridgeEvent() {
    // The test tracerProvider has a sampler which records and samples server spans, and drops all
    // others. We create a non-recording span by setting kind to INTERNAL.
    Span span = tracer.spanBuilder("span").setSpanKind(SpanKind.INTERNAL).startSpan();
    try (Scope unused = span.makeCurrent()) {
      eventLogger
          .builder("my.event-name")
          .setTimestamp(100, TimeUnit.NANOSECONDS)
          .setSeverity(Severity.DEBUG)
          .put("foo", "bar")
          .put("number", 1)
          .put("map", Value.of(Collections.singletonMap("key", Value.of("value"))))
          .setAttributes(Attributes.builder().put("color", "red").build())
          .setAttributes(Attributes.builder().put("shape", "square").build())
          .emit();
    } finally {
      span.end();
    }

    assertThat(spanExporter.getFinishedSpanItems())
        .allSatisfy(spanData -> assertThat(spanData.getEvents()).isEmpty());
  }

  @Test
  void differentSpanContext_doesNotBridgeEvent() {
    // The test tracerProvider has a sampler which records and samples SERVER spans, and drops all
    // others. We create a recording span by setting kind to SERVER.
    Span span = tracer.spanBuilder("span").setSpanKind(SpanKind.SERVER).startSpan();
    try (Scope unused = span.makeCurrent()) {
      eventLogger
          .builder("my.event-name")
          // Manually override the context
          .setContext(
              Span.wrap(
                      SpanContext.create(
                          IdGenerator.random().generateTraceId(),
                          IdGenerator.random().generateSpanId(),
                          TraceFlags.getDefault(),
                          TraceState.getDefault()))
                  .storeInContext(Context.current()))
          .setTimestamp(100, TimeUnit.NANOSECONDS)
          .setSeverity(Severity.DEBUG)
          .put("foo", "bar")
          .put("number", 1)
          .put("map", Value.of(Collections.singletonMap("key", Value.of("value"))))
          .setAttributes(Attributes.builder().put("color", "red").build())
          .setAttributes(Attributes.builder().put("shape", "square").build())
          .emit();
    } finally {
      span.end();
    }

    assertThat(spanExporter.getFinishedSpanItems())
        .allSatisfy(spanData -> assertThat(spanData.getEvents()).isEmpty());
  }

  @Test
  void noSpan_doesNotBridgeEvent() {
    eventLogger
        .builder("my.event-name")
        .setTimestamp(100, TimeUnit.NANOSECONDS)
        .setSeverity(Severity.DEBUG)
        .put("foo", "bar")
        .put("number", 1)
        .put("map", Value.of(Collections.singletonMap("key", Value.of("value"))))
        .setAttributes(Attributes.builder().put("color", "red").build())
        .setAttributes(Attributes.builder().put("shape", "square").build())
        .emit();

    assertThat(spanExporter.getFinishedSpanItems()).isEmpty();
  }
}
