/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import static java.util.Collections.singletonMap;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Implementation of the AWS X-Ray Trace Header propagation protocol but with special handling for
 * Lambda's {@code _X_AMZN_TRACE_ID} environment variable and {@code com.amazonaws.xray.traceHeader}
 * system property.
 *
 * <p>To register the X-Ray propagator together with default propagator when using the SDK:
 *
 * <pre>{@code
 * OpenTelemetrySdk.builder()
 *   .setPropagators(
 *     ContextPropagators.create(
 *         TextMapPropagator.composite(
 *             W3CTraceContextPropagator.getInstance(),
 *             AwsXrayLambdaPropagator.getInstance())))
 *    .build();
 * }</pre>
 */
public final class AwsXrayLambdaPropagator implements TextMapPropagator {

  private static final String AWS_TRACE_HEADER_ENV_KEY = "_X_AMZN_TRACE_ID";
  private static final String AWS_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
  private static final String AWS_TRACE_HEADER_PROP_LOWERCASE =
      AWS_TRACE_HEADER_PROP.toLowerCase(Locale.ROOT);
  private final AwsXrayPropagator xrayPropagator = AwsXrayPropagator.getInstance();
  private static final AwsXrayLambdaPropagator INSTANCE = new AwsXrayLambdaPropagator();

  private AwsXrayLambdaPropagator() {
    // singleton
  }

  public static AwsXrayLambdaPropagator getInstance() {
    return INSTANCE;
  }

  @Override
  public List<String> fields() {
    return xrayPropagator.fields();
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
    xrayPropagator.inject(context, carrier, setter);
  }

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
    Context xrayContext = xrayPropagator.extract(context, carrier, getter);

    if (Span.fromContext(context).getSpanContext().isValid()) {
      return xrayContext;
    }

    String traceHeader = getTraceHeaderFromCarrier(carrier, getter);
    if (isEmptyOrNull(traceHeader)) {
      traceHeader = System.getProperty(AWS_TRACE_HEADER_PROP);
    }
    if (isEmptyOrNull(traceHeader)) {
      traceHeader = System.getenv(AWS_TRACE_HEADER_ENV_KEY);
    }
    if (isEmptyOrNull(traceHeader)) {
      return xrayContext;
    }
    return xrayPropagator.extract(
        xrayContext,
        singletonMap(AwsXrayPropagator.TRACE_HEADER_KEY, traceHeader),
        MapGetter.INSTANCE);
  }

  @Override
  public String toString() {
    return "AwsXrayLambdaPropagator";
  }

  @Nullable
  private static <C> String getTraceHeaderFromCarrier(
      @Nullable C carrier, TextMapGetter<C> getter) {
    if (carrier == null || getter == null) {
      return null;
    }
    String traceHeader = getter.get(carrier, AWS_TRACE_HEADER_PROP);
    return !isEmptyOrNull(traceHeader)
        ? traceHeader
        : getter.get(carrier, AWS_TRACE_HEADER_PROP_LOWERCASE);
  }

  private static boolean isEmptyOrNull(@Nullable String value) {
    return value == null || value.isEmpty();
  }

  private enum MapGetter implements TextMapGetter<Map<String, String>> {
    INSTANCE;

    @Override
    public Set<String> keys(Map<String, String> map) {
      return map.keySet();
    }

    @Override
    @Nullable
    public String get(@Nullable Map<String, String> map, String s) {
      return map == null ? null : map.get(s);
    }
  }
}
