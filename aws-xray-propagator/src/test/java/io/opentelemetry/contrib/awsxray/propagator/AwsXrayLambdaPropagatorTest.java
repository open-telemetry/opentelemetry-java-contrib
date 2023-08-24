/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import static io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator.TRACE_HEADER_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
class AwsXrayLambdaPropagatorTest extends AwsXrayPropagatorTest {

  @SystemStub final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @SystemStub final SystemProperties systemProperties = new SystemProperties();

  @Override
  TextMapPropagator propagator() {
    return AwsXrayLambdaPropagator.getInstance();
  }

  @Test
  void extract_fromEnvironmentVariable() {
    Map<String, String> carrier =
        Collections.singletonMap(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-240000000000000000000001;Parent=1600000000000001;Sampled=1");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000000-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000240000000000000000000002",
                "1600000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_fromSystemProperty() {
    Map<String, String> carrier =
        Collections.singletonMap(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-240000000000000000000001;Parent=1600000000000001;Sampled=1");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000000-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Bar");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000240000000000000000000002",
                "1600000000000002",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }

  @Test
  void extract_systemPropertyBeforeEnvironmentVariable() {
    Map<String, String> carrier =
        Collections.singletonMap(
            TRACE_HEADER_KEY,
            "Root=1-8a3c60f7-240000000000000000000001;Parent=1600000000000001;Sampled=1");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-00000000-240000000000000000000002;Parent=1600000000000002;Sampled=1;Foo=Bar");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-00000000-240000000000000000000003;Parent=1600000000000003;Sampled=1;Foo=Baz");

    assertThat(getSpanContext(subject.extract(Context.current(), carrier, GETTER)))
        .isEqualTo(
            SpanContext.createFromRemoteParent(
                "00000000240000000000000000000003",
                "1600000000000003",
                TraceFlags.getSampled(),
                TraceState.getDefault()));
  }
}
