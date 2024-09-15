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
import java.util.Objects;
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

    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(getClass().getClassLoader())
            .addPropertiesCustomizer(
                configProperties -> {
                  // The OTel SDK by default sends data to the OTLP gRPC endpoint at localhost:4317.
                  // Change this behavior to disable by default the OTel SDK in the Maven extension
                  // so
                  // that it must be explicitly enabled by the user.
                  // To change this default behavior, we set "otel.[traces,metrics,logs].exporter"
                  // to
                  // "none" if the endpoint has not been specified
                  if (configProperties.getString("otel.exporter.otlp.endpoint") == null) {
                    Map<String, String> properties = new HashMap<>();
                    if (Objects.equals(
                            "otlp", configProperties.getString("otel.traces.exporter", "otlp"))
                        && configProperties.getString("otel.exporter.otlp.traces.endpoint")
                            == null) {
                      properties.put("otel.traces.exporter", "none");
                    }
                    if (Objects.equals(
                            "otlp", configProperties.getString("otel.metrics.exporter", "otlp"))
                        && configProperties.getString("otel.exporter.otlp.metrics.endpoint")
                            == null) {
                      properties.put("otel.metrics.exporter", "none");
                    }
                    if (Objects.equals(
                            "otlp", configProperties.getString("otel.logs.exporter", "otlp"))
                        && configProperties.getString("otel.exporter.otlp.logs.endpoint") == null) {
                      properties.put("otel.logs.exporter", "none");
                    }
                    return properties;
                  } else {
                    return Collections.emptyMap();
                  }
                })
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

    if (this.resource == null) {
      this.resource = Resource.empty();
    }
    if (this.configProperties == null) {
      this.configProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
    }

    this.openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();

    logger.debug("OpenTelemetry: OpenTelemetrySdkService initialized, resource:{}", resource);

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
