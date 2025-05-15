/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

  @VisibleForTesting final Resource resource;

  private final ConfigProperties configProperties;

  private final Tracer tracer;

  private final boolean mojosInstrumentationEnabled;

  private final boolean transferInstrumentationEnabled;

  private boolean disposed;

  public OpenTelemetrySdkService() {
    logger.info(
        "OpenTelemetry: Initialize OpenTelemetrySdkService v{}...",
        MavenOtelSemanticAttributes.TELEMETRY_DISTRO_VERSION_VALUE);

    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesCustomizer(
                OpenTelemetrySdkService::requireExplicitConfigOfTheOtlpExporter)
            .disableShutdownHook()
            .build();

    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    this.configProperties =
        Optional.ofNullable(AutoConfigureUtil.getConfig(autoConfiguredOpenTelemetrySdk))
            .orElseGet(() -> DefaultConfigProperties.createFromMap(Collections.emptyMap()));

    this.resource = AutoConfigureUtil2.getResource(autoConfiguredOpenTelemetrySdk);
    // Display resource attributes in debug logs for troubleshooting when traces are not found in
    // the observability backend, helping understand `service.name`, `service.namespace`, etc.
    logger.debug("OpenTelemetry: OpenTelemetrySdkService initialized, resource:{}", resource);

    this.mojosInstrumentationEnabled =
        configProperties.getBoolean("otel.instrumentation.maven.mojo.enabled", true);
    this.transferInstrumentationEnabled =
        configProperties.getBoolean("otel.instrumentation.maven.transfer.enabled", false);

    this.tracer = openTelemetrySdk.getTracer("io.opentelemetry.contrib.maven", VERSION);
  }

  /**
   * The OTel SDK by default sends data to the OTLP gRPC endpoint localhost:4317 if no exporter and
   * no OTLP exporter endpoint are defined. This is not suited for a build tool for which we want
   * the OTel SDK to be disabled by default.
   *
   * <p>Change the OTel SDL behavior: if none of the exporter and the OTLP exporter endpoint are
   * defined, explicitly disable the exporter setting "{@code
   * otel.[traces,metrics,logs].exporter=none}"
   *
   * @return The properties to be returned by {@link
   *     io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder#addPropertiesCustomizer(java.util.function.Function)}
   */
  static Map<String, String> requireExplicitConfigOfTheOtlpExporter(
      ConfigProperties configProperties) {

    Map<String, String> properties = new HashMap<>();
    if (configProperties.getString("otel.exporter.otlp.endpoint") != null) {
      logger.debug("OpenTelemetry: OTLP exporter endpoint is explicitly configured");
      return properties;
    }
    String[] signalTypes = {"traces", "metrics", "logs"};
    for (String signalType : signalTypes) {
      boolean isExporterImplicitlyConfiguredToOtlp =
          configProperties.getString("otel." + signalType + ".exporter") == null;
      boolean isOtlpExporterEndpointSpecified =
          configProperties.getString("otel.exporter.otlp." + signalType + ".endpoint") != null;

      if (isExporterImplicitlyConfiguredToOtlp && !isOtlpExporterEndpointSpecified) {
        logger.debug(
            "OpenTelemetry: Disabling default OTLP exporter endpoint for signal {} exporter",
            signalType);
        properties.put("otel." + signalType + ".exporter", "none");
      }
    }

    return properties;
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

  public ConfigProperties getConfigProperties() {
    return configProperties;
  }

  /** Returns the {@link ContextPropagators} for this {@link OpenTelemetry}. */
  public ContextPropagators getPropagators() {
    return this.openTelemetrySdk.getPropagators();
  }

  public boolean isMojosInstrumentationEnabled() {
    return mojosInstrumentationEnabled;
  }

  public boolean isTransferInstrumentationEnabled() {
    return transferInstrumentationEnabled;
  }
}
