/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfrevent;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JfrSpanProcessorTest {

  private static final String OPERATION_NAME = "Test Span";

  private SdkTracerProvider sdkTracerProvider;
  private Tracer tracer;

  @BeforeEach
  void setUp() {
    sdkTracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(new JfrSpanProcessor()).build();
    tracer = sdkTracerProvider.get("JfrSpanProcessorTest");
  }

  @AfterEach
  void tearDown() {
    sdkTracerProvider.shutdown();
  }

  static {
    ContextStorage.addWrapper(JfrContextStorageWrapper::new);
  }

  /**
   * Test basic single span.
   *
   * @throws java.io.IOException on io error
   */
  @Test
  void basicSpan() throws IOException {
    Path output = Files.createTempFile("test-basic-span", ".jfr");

    try {
      Recording recording = new Recording();
      recording.start();
      Span span;

      try (recording) {

        span = tracer.spanBuilder(OPERATION_NAME).setNoParent().startSpan();
        span.end();

        recording.dump(output);
      }

      List<RecordedEvent> events = RecordingFile.readAllEvents(output);
      assertThat(events).hasSize(1);
      assertThat(events)
          .extracting(e -> e.getValue("traceId"))
          .isEqualTo(span.getSpanContext().getTraceId());
      assertThat(events)
          .extracting(e -> e.getValue("spanId"))
          .isEqualTo(span.getSpanContext().getSpanId());
      assertThat(events).extracting(e -> e.getValue("operationName")).isEqualTo(OPERATION_NAME);
      assertThat(events)
          .extracting(
              e ->
                  e.getFields().stream()
                      .filter(f -> f.getName().equals("operationName"))
                      .map(d -> d.getAnnotation(Contextual.class))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList()))
          .isNotEmpty();
    } finally {
      Files.delete(output);
    }
  }

  /**
   * Test basic single span with a scope.
   *
   * @throws java.io.IOException on io error
   * @throws java.lang.InterruptedException interrupted sleep
   */
  @Test
  void basicSpanWithScope() throws IOException, InterruptedException {
    Path output = Files.createTempFile("test-basic-span-with-scope", ".jfr");

    try {
      Recording recording = new Recording();
      recording.start();
      Span span;

      try (recording) {
        span = tracer.spanBuilder(OPERATION_NAME).setNoParent().startSpan();
        try (Scope s = span.makeCurrent()) {
          Thread.sleep(10);
        }
        span.end();

        recording.dump(output);
      }

      List<RecordedEvent> events = RecordingFile.readAllEvents(output);
      assertThat(events).hasSize(2);
      assertThat(events)
          .extracting(e -> e.getValue("traceId"))
          .isEqualTo(span.getSpanContext().getTraceId());
      assertThat(events)
          .extracting(e -> e.getValue("spanId"))
          .isEqualTo(span.getSpanContext().getSpanId());
      assertThat(events)
          .filteredOn(e -> "Span".equals(e.getEventType().getLabel()))
          .extracting(e -> e.getValue("operationName"))
          .isEqualTo(OPERATION_NAME);
      assertThat(events)
          .filteredOn(e -> "Scope".equals(e.getEventType().getLabel()))
          .extracting(
              e ->
                  e.getFields().stream()
                      .filter(f -> f.getName().equals("traceId"))
                      .map(d -> d.getAnnotation(Contextual.class))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList()))
          .isNotEmpty();

    } finally {
      Files.delete(output);
    }
  }
}
