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
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to configure the {@link OpenTelemetry} instance.
 *
 * <p>Rely on the OpenTelemetry SDK AutoConfiguration extension. Parameters are passed as system
 * properties.
 *
 * <p>TODO: verify how we could use a composite {@link
 * io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties} combining the config passed by JVM
 * system properties and environment variables with overrides injected by the Otel Maven Extension
 */
@Component(role = OpenTelemetrySdkService.class, hint = "opentelemetry-service")
public final class OpenTelemetrySdkService implements Initializable, Disposable {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetrySdkService.class);

  @Requirement private RuntimeInformation runtimeInformation;

  private OpenTelemetry openTelemetry = OpenTelemetry.noop();
  private OpenTelemetrySdk openTelemetrySdk;

  private Tracer tracer;

  private SpanExporter spanExporter;

  private boolean mojosInstrumentationEnabled;

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
    if (this.openTelemetrySdk != null) {
      logger.debug("OpenTelemetry: Shutdown SDK Trace Provider...");
      final CompletableResultCode sdkProviderShutdown =
          this.openTelemetrySdk.getSdkTracerProvider().shutdown();
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
    this.openTelemetry = null;

    logger.debug("OpenTelemetry: OpenTelemetrySdkService disposed");
  }

  @Override
  public void initialize() throws InitializationException {
    logger.debug("OpenTelemetry: initialize OpenTelemetrySdkService...");
    if (StringUtils.isBlank(
        OtelUtils.getSystemPropertyOrEnvironmentVariable(
            "otel.exporter.otlp.endpoint", "OTEL_EXPORTER_OTLP_ENDPOINT", null))) {
      logger.debug(
          "OpenTelemetry: No -Dotel.exporter.otlp.endpoint property or OTEL_EXPORTER_OTLP_ENDPOINT "
              + "environment variable found, use a NOOP OpenTelemetry SDK");
    } else {
      {
        // Don't use a {@code io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider} to inject
        // Maven runtime attributes due to a classloading issue when loading the Maven OpenTelemetry
        // extension as a pom.xml {@code <extension>}.
        String initialCommaSeparatedAttributes =
            OtelUtils.getSystemPropertyOrEnvironmentVariable(
                "otel.resource.attributes", "OTEL_RESOURCE_ATTRIBUTES", "");
        Map<String, String> attributes =
            OtelUtils.getCommaSeparatedMap(initialCommaSeparatedAttributes);

        // service.name
        String serviceName =
            OtelUtils.getSystemPropertyOrEnvironmentVariable(
                "otel.service.name", "OTEL_SERVICE_NAME", null);

        if (!attributes.containsKey(ResourceAttributes.SERVICE_NAME.getKey())
            && StringUtils.isBlank(serviceName)) {
          // service.name is not defined in passed configuration, we define it
          attributes.put(
              ResourceAttributes.SERVICE_NAME.getKey(),
              MavenOtelSemanticAttributes.ServiceNameValues.SERVICE_NAME_VALUE);
        }

        // service.version
        final String mavenVersion = this.runtimeInformation.getMavenVersion();
        if (!attributes.containsKey(ResourceAttributes.SERVICE_VERSION.getKey())) {
          attributes.put(ResourceAttributes.SERVICE_VERSION.getKey(), mavenVersion);
        }

        String newCommaSeparatedAttributes = OtelUtils.getCommaSeparatedString(attributes);
        logger.debug(
            "OpenTelemetry: Initial resource attributes: {}", initialCommaSeparatedAttributes);
        logger.debug("OpenTelemetry: Use resource attributes: {}", newCommaSeparatedAttributes);
        System.setProperty("otel.resource.attributes", newCommaSeparatedAttributes);
      }

      this.openTelemetrySdk = OpenTelemetrySdkAutoConfiguration.initialize(false);
      this.openTelemetry = this.openTelemetrySdk;
    }

    String mojosInstrumentationEnabledAsString =
        System.getProperty(
            "otel.instrumentation.maven.mojo.enabled",
            System.getenv("OTEL_INSTRUMENTATION_MAVEN_MOJO_ENABLED"));
    this.mojosInstrumentationEnabled =
        Boolean.parseBoolean(
            StringUtils.defaultIfBlank(mojosInstrumentationEnabledAsString, "true"));

    this.tracer = this.openTelemetry.getTracer("io.opentelemetry.contrib.maven");
  }

  public Tracer getTracer() {
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
}
