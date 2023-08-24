/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Implementation of the AWS X-Ray Trace Header propagation protocol only uses Lambda's {@code
 * _X_AMZN_TRACE_ID} environment variable and {@code com.amazonaws.xray.traceHeader} system property
 * as the carrier. Carrier passed into {@link #inject(Context, Object, TextMapSetter)} and {@link
 * #extract(Context, Object, TextMapGetter)} are ignored.
 *
 * <p>To register the X-Ray propagator together with default propagator when using the SDK:
 *
 * <pre>{@code
 * OpenTelemetrySdk.builder()
 *   .setPropagators(
 *     ContextPropagators.create(
 *         TextMapPropagator.composite(
 *             W3CTraceContextPropagator.getInstance(),
 *             AwsXrayEnvPropagator.getInstance())))
 *    .build();
 * }</pre>
 */
public final class AwsXrayEnvPropagator implements TextMapPropagator {

  private static final String AWS_TRACE_HEADER_ENV_KEY = "_X_AMZN_TRACE_ID";
  private static final String AWS_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
  private final AwsXrayPropagator xrayPropagator = AwsXrayPropagator.getInstance();

  private static final AwsXrayEnvPropagator INSTANCE = new AwsXrayEnvPropagator();

  private AwsXrayEnvPropagator() {
    // singleton
  }

  public static AwsXrayEnvPropagator getInstance() {
    return INSTANCE;
  }

  @Override
  public List<String> fields() {
    return xrayPropagator.fields();
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {}

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> ignored) {
    String traceHeader = System.getProperty(AWS_TRACE_HEADER_PROP);
    if (isEmptyOrNull(traceHeader)) {
      traceHeader = System.getenv(AWS_TRACE_HEADER_ENV_KEY);
    }
    if (isEmptyOrNull(traceHeader)) {
      return context;
    }
    return xrayPropagator.extract(
        context,
        Collections.singletonMap(AwsXrayPropagator.TRACE_HEADER_KEY, traceHeader),
        MapGetter.INSTANCE);
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
