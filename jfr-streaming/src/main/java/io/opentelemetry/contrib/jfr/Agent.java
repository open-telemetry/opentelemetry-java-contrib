/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr;

import static io.opentelemetry.sdk.metrics.common.InstrumentType.*;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.view.Aggregation;
import io.opentelemetry.sdk.metrics.view.InstrumentSelector;
import io.opentelemetry.sdk.metrics.view.View;
import io.opentelemetry.sdk.resources.Resource;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

public final class Agent {
  private static final String OTLP_URL = "OTLP_URL";
  private static final String API_KEY = "API_KEY";
  private static final String SERVICE_NAME = "SERVICE_NAME";
  private static final String API_KEY_HEADER = "api-key";
  private static final String SERVICE_NAME_HEADER = "service.name";
  private static final String DEFAULT_URL = "http://localhost:4317";
  private static final String DEFAULT_SERVICE_NAME = "jfr-otlp-bridge";
  private static final long EXPORT_MILLIS = 2000;

  private static MeterProvider meterProvider;

  public static void agentmain(String agentArgs, Instrumentation inst) {
    premain(agentArgs, inst);
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    meterProvider = GlobalMeterProvider.get();
    if (meterProvider == null) {
      configureOpenTelemetry();
    }
    JfrMetrics.enableJfr(meterProvider);
  }

  static void configureOpenTelemetry() {
    // Configure the meter provider
    var serviceName = System.getenv(SERVICE_NAME);
    if (serviceName == null) {
      serviceName = DEFAULT_SERVICE_NAME;
    }
    var resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder().put(SERVICE_NAME_HEADER, serviceName).build()));
    var apiKey = System.getenv(API_KEY);
    var otlpUrl = System.getenv(OTLP_URL);
    if (otlpUrl == null) {
      otlpUrl = DEFAULT_URL;
    }

    var exporterBuilder = OtlpGrpcMetricExporter.builder().setEndpoint(otlpUrl);

    if (apiKey != null) {
      exporterBuilder.addHeader(API_KEY_HEADER, apiKey);
    }
    var meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);
    // Set delta aggregation
    setAggregatorFactory(meterProviderBuilder, COUNTER, Aggregation.sum(DELTA));
    setAggregatorFactory(meterProviderBuilder, UP_DOWN_COUNTER, Aggregation.sum(DELTA));
    setAggregatorFactory(meterProviderBuilder, OBSERVABLE_SUM, Aggregation.histogram());
    setAggregatorFactory(meterProviderBuilder, OBSERVABLE_UP_DOWN_SUM, Aggregation.histogram());

    var factory =
        PeriodicMetricReader.create(exporterBuilder.build(), Duration.ofMillis(EXPORT_MILLIS));

    meterProvider = meterProviderBuilder.registerMetricReader(factory).buildAndRegisterGlobal();
  }

  private static void setAggregatorFactory(
      SdkMeterProviderBuilder builder, InstrumentType instrumentType, Aggregation aggregation) {
    builder.registerView(
        InstrumentSelector.builder().setInstrumentType(instrumentType).build(),
        View.builder().setAggregation(aggregation).build());
  }
}
