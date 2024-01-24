/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.util.Collections;
import java.util.Map;

import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator.TRACE_HEADER_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemStubsExtension.class)
class AwsXrayLambdaPropagatorTest extends AwsXrayPropagatorTest {

  @SystemStub final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @SystemStub final SystemProperties systemProperties = new SystemProperties();

  private Tracer tracer;

  @Override
  TextMapPropagator propagator() {
    return AwsXrayLambdaPropagator.getInstance();
  }

  @BeforeEach
  public void setup() {
    tracer = SdkTracerProvider.builder().build().get("awsxray");
  }

  @Test
  void extract_fromEnvironmentVariable() {
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000001-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(
        getSpanContext(propagator().extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000001d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_fromSystemProperty() {
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000001-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(
        getSpanContext(propagator().extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000001d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_systemPropertyBeforeEnvironmentVariable() {
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000001-240000000000000000000001;Parent=1600000000000001;Sampled=1;Foo=Bar");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000002-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Baz");

    assertThat(
        getSpanContext(propagator().extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000002240000000000000000000002",
                "1600000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void addLink_SystemProperty() {
    Map<String, String> carrier =
        Collections.singletonMap(
            TRACE_HEADER_KEY,
            "Root=1-00000001-240000000000000000000001;Parent=1600000000000001;Sampled=1");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000002-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Bar");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000003-240000000000000000000003;Parent=1600000000000003;Sampled=1;Foo=Baz");

    Context extract = propagator().extract(Context.current(), carrier, GETTER);
    ReadableSpan span = (ReadableSpan) tracer.spanBuilder("test")
        .setParent(extract)
        .addLink(Span.fromContext(propagator().extract(extract, carrier, GETTER)).getSpanContext())
        .startSpan();
    assertThat(span.getParentSpanContext())
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000003240000000000000000000003",
                "1600000000000003",
                TraceFlags.getSampled(),
                TraceState.getDefault()));

    assertThat(span.toSpanData().getLinks())
        .isEqualTo(Collections.singletonList(LinkData.create(
            SpanContext.createFromRemoteParent(
                "00000001240000000000000000000001",
                "1600000000000001",
                TraceFlags.getSampled(),
                TraceState.getDefault()))));

  }
}
