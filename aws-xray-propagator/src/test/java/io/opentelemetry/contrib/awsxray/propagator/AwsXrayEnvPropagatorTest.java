/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator.TRACE_HEADER_KEY;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.GETTER;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.SETTER;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.SPAN_ID;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.TRACE_ID;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.getSpanContext;
import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagatorTest.withSpanContext;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
class AwsXrayEnvPropagatorTest {
  private final AwsXrayEnvPropagator subject = AwsXrayEnvPropagator.getInstance();

  @SystemStub final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @SystemStub final SystemProperties systemProperties = new SystemProperties();

  @Test
  void fields_valid() {
    assertThat(subject.fields()).containsOnly("X-Amzn-Trace-Id");
  }

  @Test
  void inject_doesNothing() {
    Map<String, String> carrier = new LinkedHashMap<>();
    subject.inject(
        withSpanContext(
            SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()),
            Context.current()),
        carrier,
        SETTER);

    assertThat(carrier).isEmpty();
  }

  @Test
  void extract_carrierIgnored() {
    Map<String, String> carrier =
        Collections.singletonMap(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1");
    assertThat(subject.extract(Context.current(), carrier, GETTER)).isEqualTo(Context.current());
  }

  @Test
  void extract_fromEnvironmentVariable() {
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000000-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_fromSystemProperty() {
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000000-d188f8fa79d48a391a778fa6;Parent=53995c3f42cd8ad8;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000d188f8fa79d48a391a778fa6",
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_systemPropertyBeforeEnvironmentVariable() {
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000000-240000000000000000000001;Parent=1600000000000001;Sampled=1;Foo=Bar");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000000-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Baz");

    assertThat(getSpanContext(subject.extract(Context.current(), Collections.emptyMap(), GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000240000000000000000000002",
                "1600000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }
}
