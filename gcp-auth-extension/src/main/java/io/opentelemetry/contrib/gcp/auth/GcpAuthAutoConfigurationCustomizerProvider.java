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
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

  static final String QUOTA_USER_PROJECT_HEADER = "X-Goog-User-Project";
  static final String GCP_USER_PROJECT_ID_KEY = "gcp.project_id";

  /**
   * Customizes the provided {@link AutoConfigurationCustomizer}.
   *
   * <p>This method attempts to retrieve Google Application Default Credentials (ADC) and performs
   * the following: - Adds authorization headers to the configured {@link SpanExporter} based on the
   * retrieved credentials. - Adds default properties for OTLP endpoint and resource attributes for
   * GCP integration.
   *
   * @param autoConfiguration the AutoConfigurationCustomizer to customize.
   * @throws GoogleAuthException if there's an error retrieving Google Application Default
   *     Credentials.
   * @throws io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException if required options are
   *     not configured through environment variables or system properties.
   */
  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    try {
      GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
      autoConfiguration
          .addSpanExporterCustomizer(
              (exporter, configProperties) -> addAuthorizationHeaders(exporter, credentials))
          .addResourceCustomizer(GcpAuthAutoConfigurationCustomizerProvider::customizeResource);
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_RETRIEVAL, e);
    }
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE - 1;
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

  private static Map<String, String> getRequiredHeaderMap(GoogleCredentials credentials) {
    Map<String, String> gcpHeaders = new HashMap<>();
    try {
      credentials.refreshIfExpired();
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_REFRESH, e);
    }
    gcpHeaders.put(QUOTA_USER_PROJECT_HEADER, credentials.getQuotaProjectId());
    gcpHeaders.put("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue());
    return gcpHeaders;
  }

  // Updates the current resource with the attributes required for ingesting OTLP data on GCP.
  private static Resource customizeResource(Resource resource, ConfigProperties configProperties) {
    String gcpProjectId =
        ConfigurableOption.GOOGLE_CLOUD_PROJECT.getConfiguredValueWithFallback(
            () -> {
              try {
                GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
                return googleCredentials.getQuotaProjectId();
              } catch (IOException e) {
                throw new GoogleAuthException(Reason.FAILED_ADC_RETRIEVAL, e);
              }
            });

    Resource res =
        Resource.create(
            Attributes.of(AttributeKey.stringKey(GCP_USER_PROJECT_ID_KEY), gcpProjectId));
    return resource.merge(res);
  }
}
