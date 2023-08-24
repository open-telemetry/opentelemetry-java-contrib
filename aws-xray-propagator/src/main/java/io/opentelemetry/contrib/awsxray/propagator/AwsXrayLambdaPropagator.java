/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.List;
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
  private static final AwsXrayPropagator XRAY = AwsXrayPropagator.getInstance();
  private static final AwsXrayEnvPropagator XRAY_ENV = AwsXrayEnvPropagator.getInstance();
  private static final AwsXrayLambdaPropagator INSTANCE = new AwsXrayLambdaPropagator();

  private AwsXrayLambdaPropagator() {
    // singleton
  }

  public static AwsXrayLambdaPropagator getInstance() {
    return INSTANCE;
  }

  @Override
  public List<String> fields() {
    return XRAY.fields();
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
    XRAY.inject(context, carrier, setter);
    // XRAY_ENV.inject is a no-op, so no need to invoke.
  }

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
    context = XRAY.extract(context, carrier, getter);
    // Currently last one wins, so invoke XRAY_ENV second to allow parent to be overwritten.
    context = XRAY_ENV.extract(context, carrier, getter);
    return context;
  }
}
