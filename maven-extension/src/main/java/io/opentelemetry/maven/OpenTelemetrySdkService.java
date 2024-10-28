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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import java.io.Closeable;
import java.util.Collections;
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

  private Resource resource;

  private ConfigProperties configProperties;

  private final Tracer tracer;

  private final boolean mojosInstrumentationEnabled;

  private boolean disposed;

  public OpenTelemetrySdkService() {
    logger.debug(
        "OpenTelemetry: Initialize OpenTelemetrySdkService v{}...",
        MavenOtelSemanticAttributes.TELEMETRY_DISTRO_VERSION_VALUE);

    this.resource = Resource.empty();
    this.configProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());

    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesCustomizer(
                OpenTelemetrySdkService::requireExplicitConfigOfTheOtlpExporter)
            .addPropertiesCustomizer(
                config -> {
                  // keep a reference to the computed config properties for future use in the
                  // extension
                  this.configProperties = config;
                  return Collections.emptyMap();
                })
            .addResourceCustomizer(
                (res, configProperties) -> {
                  // keep a reference to the computed Resource for future use in the extension
                  this.resource = Resource.builder().putAll(res).build();
                  return this.resource;
                })
            .disableShutdownHook()
            .build();

    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();

    logger.debug("OpenTelemetry: OpenTelemetrySdkService initialized, resource:{}", resource);

    // TODO should we replace `getBooleanConfig(name)` by `configProperties.getBoolean(name)`?
    Boolean mojoSpansEnabled = getBooleanConfig("otel.instrumentation.maven.mojo.enabled");
    this.mojosInstrumentationEnabled = mojoSpansEnabled == null || mojoSpansEnabled;

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
    if (configProperties.getString("otel.exporter.otlp.endpoint") == null) {
      for (SignalType signalType : SignalType.values()) {
        boolean isExporterImplicitlyConfiguredToOtlp =
            configProperties.getString("otel." + signalType.value + ".exporter") == null;
        boolean isOtlpExporterEndpointSpecified =
            configProperties.getString("otel.exporter.otlp." + signalType.value + ".endpoint")
                != null;

        if (isExporterImplicitlyConfiguredToOtlp && !isOtlpExporterEndpointSpecified) {
          logger.debug(
              "OpenTelemetry: Disabling default OTLP exporter endpoint for signal {} exporter",
              signalType.value);
          properties.put("otel." + signalType.value + ".exporter", "none");
        }
      }
    } else {
      logger.debug("OpenTelemetry: OTLP exporter endpoint is explicitly configured");
    }
    return properties;
  }

  enum SignalType {
    TRACES("traces"),
    METRICS("metrics"),
    LOGS("logs");

    private final String value;

    SignalType(String value) {
      this.value = value;
    }
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

  public Resource getResource() {
    return resource;
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
