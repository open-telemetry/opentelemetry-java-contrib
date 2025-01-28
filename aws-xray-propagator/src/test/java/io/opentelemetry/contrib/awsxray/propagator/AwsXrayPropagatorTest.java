/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator.TRACE_HEADER_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AwsXrayPropagatorTest {

  static final String TRACE_ID = "8a3c60f7d188f8fa79d48a391a778fa6";
  static final String SPAN_ID = "53995c3f42cd8ad8";

  static final TextMapSetter<Map<String, String>> SETTER = Map::put;
  static final TextMapGetter<Map<String, String>> GETTER =
      new TextMapGetter<Map<String, String>>() {
        @Override
        public Set<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Nullable
        @Override
        public String get(Map<String, String> carrier, String key) {
          return carrier.get(key);
        }
      };
  protected static final AwsXrayPropagator X_RAY = AwsXrayPropagator.getInstance();
  protected final TextMapPropagator subject = propagator();

  TextMapPropagator propagator() {
    return AwsXrayPropagator.getInstance();
  }

  @Test
  void fields_valid() {
    assertThat(subject.fields()).contains("X-Amzn-Trace-Id");
  }

  @Test
  void inject_SampledContext() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
            SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()),
            Context.current()),
        carrier,
        SETTER);

    assertThat(carrier)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1");
  }

  @Test
  void inject_NotSampledContext() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
            SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
            Context.current()),
        carrier,
        SETTER);

    assertThat(carrier)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");
  }

  @Test
  void inject_WithBaggage() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
                SpanContext.create(
                    TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
                Context.current())
            .with(Baggage.builder().put("cat", "ignored").put("dog", "ignored").build()),
        carrier,
        SETTER);

    // all non-lineage baggage is dropped from trace header
    assertThat(carrier)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");
  }

  @Test
  void inject_WithLineage() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
                SpanContext.create(
                    TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
                Context.current())
            .with(Baggage.builder().put("Lineage", "32767:e65a2c4d:255").build()),
        carrier,
        SETTER);

    assertThat(carrier)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0;Lineage=32767:e65a2c4d:255");
  }

  @Test
  void inject_WithTraceState() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
            SpanContext.create(
                TRACE_ID,
                SPAN_ID,
                TraceFlags.getDefault(),
                TraceState.builder().put("foo", "bar").build()),
            Context.current()),
        carrier,
        SETTER);

    // TODO: assert trace state when the propagator supports it, for general key/value pairs we are
    // mapping with baggage.
    assertThat(carrier)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");
  }

  @Test
  void inject_nullContext() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(null, carrier, SETTER);
    assertThat(carrier).isEmpty();
  }

  @Test
  void inject_nullSetter() {
    Map<String, String> carrier = new LinkedHashMap<>();
    Context context =
        withSpanContext(
            SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
            Context.current());
    subject.inject(context, carrier, null);
    assertThat(carrier).isEmpty();
  }

  @Test
  void extract_Nothing() {
    // Context remains untouched.
    assertThat(subject.extract(Context.current(), Collections.<String, String>emptyMap(), GETTER))
        .isSameAs(Context.current());
  }

  @Test
  void extract_SampledContext() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()));
  }

  @Test
  void extract_NotSampledContext() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()));
  }

  @Test
  void extract_DifferentPartOrder() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Parent=53995c3f42cd8ad8;Sampled=1;Root=1-8a3c60f7-d188f8fa79d48a391a778fa6");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()));
  }

  @Test
  void extract_WithLineage() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=32767:e65a2c4d:255");

    Context context = subject.extract(Context.current(), carrier, GETTER);
    assertThat(getSpanContext(context))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()));
    assertThat(Baggage.fromContext(context).getEntryValue("Lineage"))
        .isEqualTo("32767:e65a2c4d:255");
  }

  @Test
  void extract_AddedLineagePreservesExistingBaggage() {
    Baggage expectedBaggage =
        Baggage.builder()
            .put("cat", "meow")
            .put("dog", "bark")
            .put("Lineage", "32767:e65a2c4d:255")
            .build();
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=32767:e65a2c4d:255");

    Context context =
        subject.extract(
            Context.current().with(Baggage.builder().put("cat", "meow").put("dog", "bark").build()),
            carrier,
            GETTER);
    assertThat(getSpanContext(context))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()));

    assertThat(Baggage.fromContext(context).asMap()).isEqualTo(expectedBaggage.asMap());
  }

  @Test
  void extract_inject_ValidTraceHeader() {
    Map<String, String> carrier1 = new LinkedHashMap<>();
    carrier1.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=32767:e65a2c4d:255");

    Context context = subject.extract(Context.current(), carrier1, GETTER);

    // inject extracted trace context into new trace header
    Map<String, String> carrier2 = new LinkedHashMap<>();
    subject.inject(context, carrier2, SETTER);

    assertThat(carrier2)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=32767:e65a2c4d:255");
  }

  @Test
  void extract_inject_InvalidLineage() {
    Map<String, String> carrier1 = new LinkedHashMap<>();
    carrier1.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=1:badc0de:13");

    Context context = subject.extract(Context.current(), carrier1, GETTER);

    // inject extracted trace context into new trace header
    Map<String, String> carrier2 = new LinkedHashMap<>();
    subject.inject(context, carrier2, SETTER);

    assertThat(carrier2)
        .containsEntry(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1");
  }

  @Test
  void extract_EmptyHeaderValue() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(TRACE_HEADER_KEY, "");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidTraceId() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=abcdefghijklmnopabcdefghijklmnop;Parent=53995c3f42cd8ad8;Sampled=0");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidTraceId_Size_TooBig() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa600;Parent=53995c3f42cd8ad8;Sampled=0");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidTraceId_Size_TooShort() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-64fbd5a9-2202432c9dfed25ae1e6996;Parent=53995c3f42cd8ad8;Sampled=0");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidSpanId() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=abcdefghijklmnop;Sampled=0");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidSpanId_Size() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad800;Sampled=0");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidFlags() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidFlags_Size() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=10220");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_InvalidFlags_NonNumeric() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=a");

    verifyInvalidBehavior(invalidHeaders);
  }

  @Test
  void extract_Invalid_NoSpanId() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>(1);
    invalidHeaders.put(TRACE_HEADER_KEY, "Root=1-622422bf-59625fe25708d4660735d8ef");

    verifyInvalidBehavior(invalidHeaders);
  }

  private void verifyInvalidBehavior(Map<String, String> invalidHeaders) {
    Context input = Context.current();
    Context result = subject.extract(input, invalidHeaders, GETTER);
    assertThat(result).isSameAs(input);
    assertThat(getSpanContext(result)).isSameAs(SpanContext.getInvalid());
  }

  @ParameterizedTest
  @MethodSource("providesBadLineages")
  void extract_invalidLineage(String lineage) {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        String.format(
            "Root=2-1a2a3a4a-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Lineage=%s",
            lineage));
    Context context = subject.extract(Context.current(), carrier, GETTER);
    assertThat(Baggage.fromContext(context).getEntryValue("Lineage")).isNull();
  }

  static Stream<Arguments> providesBadLineages() {
    return Stream.of(
        Arguments.of("1::"),
        Arguments.of("1"),
        Arguments.of(""),
        Arguments.of(":"),
        Arguments.of("::"),
        Arguments.of("1:badc0de:13"),
        Arguments.of(":fbadc0de:13"),
        Arguments.of("1:fbadc0de:"),
        Arguments.of("1::1"),
        Arguments.of("65535:fbadc0de:255"),
        Arguments.of("-213:e65a2c4d:255"),
        Arguments.of("213:e65a2c4d:-22"));
  }

  @Test
  void extract_nullContext() {
    assertThat(subject.extract(null, Collections.emptyMap(), GETTER)).isSameAs(Context.root());
  }

  @Test
  void extract_nullGetter() {
    Context context =
        withSpanContext(
            SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
            Context.current());
    assertThat(subject.extract(context, Collections.emptyMap(), null)).isSameAs(context);
  }

  @Test
  void extract_EpochPart_ZeroedSingleDigit() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-0-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_EpochPart_TwoChars() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-1a-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "0000001ad188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_EpochPart_Zeroed() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=1-00000000-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_InvalidTraceId_EpochPart_TooLong() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY,
        "Root=1-8a3c60f711-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");

    assertThat(getSpanContext(subject.extract(Context.current(), invalidHeaders, GETTER)))
        .isSameAs(SpanContext.getInvalid());
  }

  @Test
  void extract_InvalidTraceId_EpochPart_Empty() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY, "Root=1--d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");

    assertThat(getSpanContext(subject.extract(Context.current(), invalidHeaders, GETTER)))
        .isSameAs(SpanContext.getInvalid());
  }

  @Test
  void extract_InvalidTraceId_EpochPart_Missing() {
    Map<String, String> invalidHeaders = new LinkedHashMap<>();
    invalidHeaders.put(
        TRACE_HEADER_KEY, "Root=1-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=0");

    assertThat(getSpanContext(subject.extract(Context.current(), invalidHeaders, GETTER)))
        .isSameAs(SpanContext.getInvalid());
  }

  @Test
  void extract_InvalidTraceId_WrongVersion() {
    Map<String, String> carrier = new LinkedHashMap<>();
    carrier.put(
        TRACE_HEADER_KEY,
        "Root=2-1a2a3a4a-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isSameAs(SpanContext.getInvalid());
  }

  private static Context withSpanContext(SpanContext spanContext, Context context) {
    return context.with(Span.wrap(spanContext));
  }

  SpanContext getSpanContext(Context context) {
    return Span.fromContext(context).getSpanContext();
  }
}
