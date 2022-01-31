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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import io.opentelemetry.sdk.logs.LogEmitter;
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

  @Nullable private LogEmitter logEmitter;

  private boolean mojosInstrumentationEnabled;

  /** Visible for testing */
  @Nullable AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk;

  /**
   * Note: the JVM shutdown hook defined by the {@code
   * io.opentelemetry.sdk.autoconfigure.TracerProviderConfiguration} v1.7.0 does NOT cause
   * classloading issues even when Maven Plexus has unloaded the classes of the Otel Maven Extension
   * before the shutdown hook is invoked.
   *
   * <p>TODO create a feature request on {@code
   * io.opentelemetry.sdk.autoconfigure.TracerProviderConfiguration} to support the capability to
   * not register a JVM shutdown hook at initialization time (see
   * https://github.com/open-telemetry/opentelemetry-java/blob/v1.7.0/sdk-extensions/autoconfigure/src/main/java/io/opentelemetry/sdk/autoconfigure/TracerProviderConfiguration.java#L58
   * )
   */
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
        logger.warn(
            "OpenTelemetry: Failure to shutdown SDK Trace Provider (done: "
                + sdkProviderShutdown.isDone()
                + ")");
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

    // Change default of "otel.traces.exporter" from "otlp" to "none"
    // The impacts are
    // * If no otel exporter settings are passed, then the Maven extension will not export
    //   rather than exporting on OTLP GRPC to http://localhost:4317
    // * If OTEL_EXPORTER_OTLP_ENDPOINT is defined but OTEL_TRACES_EXPORTER is not, then don't
    //   export
    Map<String, String> properties = Collections.singletonMap("otel.traces.exporter", "none");

    this.autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesSupplier(() -> properties)
            .build();

    if (logger.isDebugEnabled()) {
      logger.debug(
          "OpenTelemetry: OpenTelemetry SDK initialized with  "
              + OtelUtils.prettyPrintSdkConfiguration(autoConfiguredOpenTelemetrySdk));
    }
    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    this.openTelemetry = this.openTelemetrySdk;

    Boolean mojoSpansEnabled =
        autoConfiguredOpenTelemetrySdk
            .getConfig()
            .getBoolean("otel.instrumentation.maven.mojo.enabled");
    this.mojosInstrumentationEnabled = mojoSpansEnabled == null ? true : mojoSpansEnabled;

    this.tracer = openTelemetry.getTracer("io.opentelemetry.contrib.maven", VERSION);
    String otelLogsExporter = autoConfiguredOpenTelemetrySdk.getConfig().getString("otel.logs.exporter");
    if (otelLogsExporter != null && !otelLogsExporter.equals("none")) {
      this.logEmitter = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().getSdkLogEmitterProvider().get("io.opentelemetry.contrib.maven");
    }

  }

  public Tracer getTracer() {
    Tracer tracer = this.tracer;
    if (tracer == null) {
      throw new IllegalStateException("Not initialized");
    }
    return tracer;
  }

  /**
   *
   * @return {@code null} if no Otel Log Exporter is enabled.
   */
  @Nullable
  public LogEmitter getLogEmitter() {
    return logEmitter;
  }

  /** Returns the {@link ContextPropagators} for this {@link OpenTelemetry}. */
  public ContextPropagators getPropagators() {
    return openTelemetry.getPropagators();
  }

  public boolean isMojosInstrumentationEnabled() {
    return mojosInstrumentationEnabled;
  }
}
