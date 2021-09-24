/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.maven;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;
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
 * <p>Mimic the <a
 * href="https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure">OpenTelemetry
 * SDK Autoconfigure</a> that can't be used due to class loading issues when declaring the Maven
 * OpenTelemetry extension using the pom.xml {@code <extension>} declaration.
 *
 * <p>The OpenTelemetry SDK Autoconfigure extension registers a <a
 * href="https://github.com/open-telemetry/opentelemetry-java/blob/v1.6.0/sdk-extensions/autoconfigure/src/main/java/io/opentelemetry/sdk/autoconfigure/TracerProviderConfiguration.java#L58">
 * JVM shutdown hook on {@code SdkTracerProvider#close()}</a> that is incompatible with the fact
 * that Maven extensions are unloaded before the JVM shuts down, requiring to close the trace
 * provider earlier in the lifecycle of the Maven build, and causing {@link NoClassDefFoundError}
 * when the shutdown hook is invoked.
 */
@Component(role = OpenTelemetrySdkService.class, hint = "opentelemetry-service")
public final class OpenTelemetrySdkService implements Initializable, Disposable {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetrySdkService.class);

  @Requirement private RuntimeInformation runtimeInformation;

  private OpenTelemetry openTelemetry;
  private OpenTelemetrySdk openTelemetrySdk;

  private Tracer tracer;

  private SpanExporter spanExporter;

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

  /** TODO add support for `OTEL_EXPORTER_OTLP_CERTIFICATE` */
  @Override
  public void initialize() throws InitializationException {
    logger.debug("OpenTelemetry: initialize OpenTelemetrySdkService...");
    // OTEL_EXPORTER_OTLP_ENDPOINT
    String otlpEndpoint =
        System.getProperty(
            "otel.exporter.otlp.endpoint", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
    if (StringUtils.isBlank(otlpEndpoint)) {
      logger.debug(
          "OpenTelemetry: No -Dotel.exporter.otlp.endpoint property or OTEL_EXPORTER_OTLP_ENDPOINT environment variable found, use a NOOP tracer");
      this.openTelemetry = OpenTelemetry.noop();
    } else {
      // OtlpGrpcSpanExporterBuilder spanExporterBuilder = OtlpGrpcSpanExporter.builder();
      OtlpGrpcSpanExporterBuilder spanExporterBuilder = OtlpGrpcSpanExporter.builder();
      spanExporterBuilder.setEndpoint(otlpEndpoint);

      // OTEL_EXPORTER_OTLP_HEADERS
      String otlpExporterHeadersAsString =
          System.getProperty(
              "otel.exporter.otlp.headers", System.getenv("OTEL_EXPORTER_OTLP_HEADERS"));
      Map<String, String> otlpExporterHeaders =
          OtelUtils.getCommaSeparatedMap(otlpExporterHeadersAsString);
      otlpExporterHeaders.forEach(spanExporterBuilder::addHeader);

      // OTEL_EXPORTER_OTLP_TIMEOUT
      String otlpExporterTimeoutMillis =
          System.getProperty(
              "otel.exporter.otlp.timeout", System.getenv("OTEL_EXPORTER_OTLP_TIMEOUT"));
      if (StringUtils.isNotBlank(otlpExporterTimeoutMillis)) {
        try {
          spanExporterBuilder.setTimeout(
              Duration.ofMillis(Long.parseLong(otlpExporterTimeoutMillis)));
        } catch (NumberFormatException e) {
          logger.warn("OpenTelemetry: Skip invalid OTLP timeout " + otlpExporterTimeoutMillis, e);
        }
      }

      this.spanExporter = spanExporterBuilder.build();

      // OTEL_RESOURCE_ATTRIBUTES
      AttributesBuilder resourceAttributesBuilder = Attributes.builder();
      Resource mavenResource = getMavenResource();
      resourceAttributesBuilder.putAll(mavenResource.getAttributes());
      String otelResourceAttributesAsString =
          System.getProperty("otel.resource.attributes", System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
      if (StringUtils.isNotBlank(otelResourceAttributesAsString)) {
        Map<String, String> otelResourceAttributes =
            OtelUtils.getCommaSeparatedMap(otelResourceAttributesAsString);
        // see io.opentelemetry.sdk.autoconfigure.EnvironmentResource.getAttributes
        otelResourceAttributes.forEach(resourceAttributesBuilder::put);
      }
      final Attributes resourceAttributes = resourceAttributesBuilder.build();

      logger.debug(
          "OpenTelemetry: Export OpenTelemetry traces to {} with attributes: {}",
          otlpEndpoint,
          resourceAttributes);

      final BatchSpanProcessor batchSpanProcessor =
          BatchSpanProcessor.builder(spanExporter).build();
      SdkTracerProvider sdkTracerProvider =
          SdkTracerProvider.builder()
              .setResource(Resource.create(resourceAttributes))
              .addSpanProcessor(batchSpanProcessor)
              .build();

      this.openTelemetrySdk =
          OpenTelemetrySdk.builder()
              .setTracerProvider(sdkTracerProvider)
              .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
              .build();
      this.openTelemetry = this.openTelemetrySdk;
    }
    this.tracer = this.openTelemetry.getTracer("io.opentelemetry.contrib.maven");
  }

  public Tracer getTracer() {
    if (tracer == null) {
      throw new IllegalStateException("Not initialized");
    }
    return tracer;
  }

  /**
   * Don't use a {@code io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider} due to classloading
   * issue when loading the Maven OpenTelemetry extension as a pom.xml {@code <extension>}.
   */
  protected Resource getMavenResource() {
    final String mavenVersion = this.runtimeInformation.getMavenVersion();
    final Attributes attributes =
        Attributes.of(
            ResourceAttributes.SERVICE_NAME,
            MavenOtelSemanticAttributes.ServiceNameValues.SERVICE_NAME_VALUE,
            ResourceAttributes.SERVICE_VERSION,
            mavenVersion);
    return Resource.create(attributes);
  }

  /** Returns the {@link ContextPropagators} for this {@link OpenTelemetry}. */
  public ContextPropagators getPropagators() {
    return openTelemetry.getPropagators();
  }
}
