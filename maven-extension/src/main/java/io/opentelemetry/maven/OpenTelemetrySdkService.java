/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service to configure the {@link OpenTelemetry} instance. */
@Component(role = OpenTelemetrySdkService.class, hint = "opentelemetry-service")
public final class OpenTelemetrySdkService implements Initializable, Disposable {

  static final String VERSION =
      OpenTelemetrySdkService.class.getPackage().getImplementationVersion();

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetrySdkService.class);

  private OpenTelemetry openTelemetry = OpenTelemetry.noop();
  @Nullable private OpenTelemetrySdk openTelemetrySdk;

  @Nullable private Tracer tracer;

  private boolean mojosInstrumentationEnabled;

  /** Visible for testing */
  @Nullable AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk;

  @Override
  public synchronized void dispose() {
    logger.debug("OpenTelemetry: dispose OpenTelemetrySdkService...");
    OpenTelemetrySdk openTelemetrySdk = this.openTelemetrySdk;
    if (openTelemetrySdk != null) {
      logger.debug("OpenTelemetry: Shutdown SDK Trace Provider...");
      CompletableResultCode sdkProviderShutdown =
          openTelemetrySdk.getSdkTracerProvider().shutdown();
      sdkProviderShutdown.join(10, TimeUnit.SECONDS);
      if (sdkProviderShutdown.isSuccess()) {
        logger.debug("OpenTelemetry: SDK Trace Provider shut down");
      } else {
        logger.warn("OpenTelemetry: Failure to shutdown SDK Trace Provider (done: {})",
            sdkProviderShutdown.isDone());
      }
      this.openTelemetrySdk = null;
    }
    this.openTelemetry = OpenTelemetry.noop();

    this.autoConfiguredOpenTelemetrySdk = null;
    logger.debug("OpenTelemetry: OpenTelemetrySdkService disposed");
  }

  @Override
  public void initialize() {
    logger.debug("OpenTelemetry: Initialize OpenTelemetrySdkService v{}...", VERSION);

    // Change default of "otel.[traces,metrics,logs].exporter" from "otlp" to "none"
    // The impacts are
    // * If no otel exporter settings are passed, then the Maven extension will not export
    //   rather than exporting on OTLP GRPC to http://localhost:4317
    // * If OTEL_EXPORTER_OTLP_ENDPOINT is defined but OTEL_[TRACES,METRICS,LOGS]_EXPORTER,
    //   is not, then don't export
    Map<String, String> properties = new HashMap<>();
    properties.put("otel.traces.exporter", "none");
    properties.put("otel.metrics.exporter", "none");
    properties.put("otel.logs.exporter", "none");

    this.autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesSupplier(() -> properties)
            .disableShutdownHook()
            .build();

    if (logger.isDebugEnabled()) {
      logger.debug("OpenTelemetry: OpenTelemetry SDK initialized");
    }
    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    this.openTelemetry = this.openTelemetrySdk;

    Boolean mojoSpansEnabled = getBooleanConfig("otel.instrumentation.maven.mojo.enabled");
    this.mojosInstrumentationEnabled = mojoSpansEnabled == null ? true : mojoSpansEnabled;

    this.tracer = openTelemetry.getTracer("io.opentelemetry.contrib.maven", VERSION);
  }

  public Tracer getTracer() {
    Tracer tracer = this.tracer;
    if (tracer == null) {
      throw new IllegalStateException("Not initialized");
    }
    return tracer;
  }

  /** Returns the {@link ContextPropagators} for this {@link OpenTelemetry}. */
  public ContextPropagators getPropagators() {
    return openTelemetry.getPropagators();
  }

  public boolean isMojosInstrumentationEnabled() {
    return mojosInstrumentationEnabled;
  }

  @Nullable
  private static Boolean getBooleanConfig(String name) {
    String value = System.getProperty(name);
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    value = System.getenv(name.toUpperCase(Locale.ROOT).replace('.', '_'));
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return null;
  }
}
