/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.gcp.auth.GoogleAuthException.Reason;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * An AutoConfigurationCustomizerProvider for Google Cloud Platform (GCP) OpenTelemetry (OTLP)
 * integration.
 *
 * <p>This class is registered as a service provider using {@link AutoService} and is responsible
 * for customizing the OpenTelemetry configuration for GCP specific behavior. It retrieves Google
 * Application Default Credentials (ADC) and adds them as authorization headers to the configured
 * {@link SpanExporter}. It also sets default properties and resource attributes for GCP
 * integration.
 *
 * @see AutoConfigurationCustomizerProvider
 * @see GoogleCredentials
 */
@AutoService(AutoConfigurationCustomizerProvider.class)
public class GcpAuthAutoConfigurationCustomizerProvider
    implements AutoConfigurationCustomizerProvider {

  static final String QUOTA_USER_PROJECT_HEADER = "x-goog-user-project";
  static final String GCP_USER_PROJECT_ID_KEY = "gcp.project_id";

  private static final String SIGNAL_TYPE_TRACES = "traces";
  private static final String SIGNAL_TYPE_METRICS = "metrics";
  private static final String SIGNAL_TYPE_ALL = "all";

  /**
   * Customizes the provided {@link AutoConfigurationCustomizer} such that authenticated exports to
   * GCP Telemetry API are possible from the configured OTLP exporter.
   *
   * <p>This method attempts to retrieve Google Application Default Credentials (ADC) and performs
   * the following:
   *
   * <ul>
   *   <li>Verifies whether the configured OTLP endpoint (base or signal specific) is a known GCP
   *       endpoint.
   *   <li>If the configured base OTLP endpoint is a known GCP Telemetry API endpoint, customizes
   *       both the configured OTLP {@link SpanExporter} and {@link MetricExporter}.
   *   <li>If the configured signal specific endpoint is a known GCP Telemetry API endpoint,
   *       customizes only the signal specific exporter.
   * </ul>
   *
   * The 'customization' performed includes customizing the exporters by adding required headers to
   * the export calls made and customizing the resource by adding required resource attributes to
   * enable GCP integration.
   *
   * @param autoConfiguration the AutoConfigurationCustomizer to customize.
   * @throws GoogleAuthException if there's an error retrieving Google Application Default
   *     Credentials.
   * @throws io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException if required options are
   *     not configured through environment variables or system properties.
   */
  @Override
  public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
    GoogleCredentials credentials;
    try {
      credentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_RETRIEVAL, e);
    }
    autoConfiguration
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) -> customizeSpanExporter(spanExporter, credentials))
        .addMetricExporterCustomizer(
            (metricExporter, configProperties) ->
                customizeMetricExporter(metricExporter, credentials))
        .addResourceCustomizer(GcpAuthAutoConfigurationCustomizerProvider::customizeResource);
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE - 1;
  }

  private static SpanExporter customizeSpanExporter(
      SpanExporter exporter, GoogleCredentials credentials) {
    if (isSignalTargeted(SIGNAL_TYPE_TRACES)) {
      return addAuthorizationHeaders(exporter, credentials);
    }
    return exporter;
  }

  private static MetricExporter customizeMetricExporter(
      MetricExporter exporter, GoogleCredentials credentials) {
    if (isSignalTargeted(SIGNAL_TYPE_METRICS)) {
      return addAuthorizationHeaders(exporter, credentials);
    }
    return exporter;
  }

  // Checks if the auth extension is configured to target the passed signal for authentication.
  private static boolean isSignalTargeted(String signal) {
    String targetedSignals =
        ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getConfiguredValueWithFallback(
            () -> SIGNAL_TYPE_ALL);
    return Arrays.stream(targetedSignals.split(","))
        .map(String::trim)
        .map(
            targetedSignal ->
                targetedSignal.equals(signal) || targetedSignal.equals(SIGNAL_TYPE_ALL))
        .findFirst()
        .isPresent();
  }

  // Adds authorization headers to the calls made by the OtlpGrpcSpanExporter and
  // OtlpHttpSpanExporter.
  private static SpanExporter addAuthorizationHeaders(
      SpanExporter exporter, GoogleCredentials credentials) {
    if (exporter instanceof OtlpHttpSpanExporter) {
      OtlpHttpSpanExporterBuilder builder =
          ((OtlpHttpSpanExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials));
      return builder.build();
    } else if (exporter instanceof OtlpGrpcSpanExporter) {
      OtlpGrpcSpanExporterBuilder builder =
          ((OtlpGrpcSpanExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials));
      return builder.build();
    }
    return exporter;
  }

  // Adds authorization headers to the calls made by the OtlpGrpcMetricExporter and
  // OtlpHttpMetricExporter.
  private static MetricExporter addAuthorizationHeaders(
      MetricExporter exporter, GoogleCredentials credentials) {
    if (exporter instanceof OtlpHttpMetricExporter) {
      OtlpHttpMetricExporterBuilder builder =
          ((OtlpHttpMetricExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials));
      return builder.build();
    } else if (exporter instanceof OtlpGrpcMetricExporter) {
      OtlpGrpcMetricExporterBuilder builder =
          ((OtlpGrpcMetricExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials));
      return builder.build();
    }
    return exporter;
  }

  private static Map<String, String> getRequiredHeaderMap(GoogleCredentials credentials) {
    Map<String, List<String>> gcpHeaders;
    try {
      // this also refreshes the credentials, if required
      gcpHeaders = credentials.getRequestMetadata();
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_REFRESH, e);
    }
    // flatten list
    Map<String, String> flattenedHeaders =
        gcpHeaders.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        entry.getValue().stream()
                            .filter(Objects::nonNull) // Filter nulls
                            .filter(s -> !s.isEmpty()) // Filter empty strings
                            .collect(Collectors.joining(","))));
    // Add quota user project header if not detected by the auth library and user provided it via
    // system properties.
    if (!flattenedHeaders.containsKey(QUOTA_USER_PROJECT_HEADER)) {
      Optional<String> maybeConfiguredQuotaProjectId =
          ConfigurableOption.GOOGLE_CLOUD_QUOTA_PROJECT.getConfiguredValueAsOptional();
      maybeConfiguredQuotaProjectId.ifPresent(
          configuredQuotaProjectId ->
              flattenedHeaders.put(QUOTA_USER_PROJECT_HEADER, configuredQuotaProjectId));
    }
    return flattenedHeaders;
  }

  // Updates the current resource with the attributes required for ingesting OTLP data on GCP.
  private static Resource customizeResource(Resource resource, ConfigProperties configProperties) {
    String gcpProjectId = ConfigurableOption.GOOGLE_CLOUD_PROJECT.getConfiguredValue();
    Resource res =
        Resource.create(
            Attributes.of(AttributeKey.stringKey(GCP_USER_PROJECT_ID_KEY), gcpProjectId));
    return resource.merge(res);
  }
}
