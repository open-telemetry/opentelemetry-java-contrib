/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service to configure the {@link OpenTelemetry} instance. */
@Named
@Singleton
public final class OpenTelemetrySdkService implements Closeable {

  static final String VERSION =
      OpenTelemetrySdkService.class.getPackage().getImplementationVersion();

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetrySdkService.class);

  private final OpenTelemetrySdk openTelemetrySdk;

  private final Tracer tracer;

  private final boolean mojosInstrumentationEnabled;

  private boolean disposed;

  public OpenTelemetrySdkService() {
    logger.debug(
        "OpenTelemetry: Initialize OpenTelemetrySdkService v{}...",
        MavenOtelSemanticAttributes.TELEMETRY_DISTRO_VERSION_VALUE);

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

    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesSupplier(() -> properties)
            .disableShutdownHook()
            .build();

    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();

    Boolean mojoSpansEnabled = getBooleanConfig("otel.instrumentation.maven.mojo.enabled");
    this.mojosInstrumentationEnabled = mojoSpansEnabled == null || mojoSpansEnabled;

    this.tracer = openTelemetrySdk.getTracer("io.opentelemetry.contrib.maven", VERSION);
  }

  @PreDestroy
  @Override
  public synchronized void close() {
    if (disposed) {
      logger.debug("OpenTelemetry: OpenTelemetry SDK already shut down, ignore");
    } else {
      logger.debug("OpenTelemetry: Shutdown OpenTelemetry SDK...");
      CompletableResultCode openTelemetrySdkShutdownResult =
          this.openTelemetrySdk.shutdown().join(10, TimeUnit.SECONDS);
      if (openTelemetrySdkShutdownResult.isSuccess()) {
        logger.debug("OpenTelemetry: OpenTelemetry SDK successfully shut down");
      } else {
        logger.warn(
            "OpenTelemetry: Failure to shutdown OpenTelemetry SDK (done: {})",
            openTelemetrySdkShutdownResult.isDone());
      }
      this.disposed = true;
    }
  }

  public Tracer getTracer() {
    return this.tracer;
  }

  /** Returns the {@link ContextPropagators} for this {@link OpenTelemetry}. */
  public ContextPropagators getPropagators() {
    return this.openTelemetrySdk.getPropagators();
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
