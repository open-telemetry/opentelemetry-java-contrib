/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.auto.service.AutoService;
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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  private static final Logger logger =
      Logger.getLogger(GcpAuthAutoConfigurationCustomizerProvider.class.getName());
  private static final String SIGNAL_TARGET_WARNING_FIX_SUGGESTION =
      String.format(
          "You may safely ignore this warning if it is intentional, otherwise please configure the '%s' by exporting valid values to environment variable: %s or by setting valid values in system property: %s.",
          ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getUserReadableName(),
          ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getEnvironmentVariable(),
          ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getSystemProperty());

  static final String QUOTA_USER_PROJECT_HEADER = "x-goog-user-project";
  static final String GCP_USER_PROJECT_ID_KEY = "gcp.project_id";

  static final String SIGNAL_TYPE_TRACES = "traces";
  static final String SIGNAL_TYPE_METRICS = "metrics";
  static final String SIGNAL_TYPE_ALL = "all";
  static final String SIGNAL_TYPE_NONE = "none";

  static final String TOKEN_TYPE_ACCESS_TOKEN = "access_token";
  static final String TOKEN_TYPE_ID_TOKEN = "id_token";

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
    LazyCredentialsSupplier credentialsSupplier = new LazyCredentialsSupplier();
    autoConfiguration
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) ->
                customizeSpanExporter(spanExporter, credentialsSupplier, configProperties))
        .addMetricExporterCustomizer(
            (metricExporter, configProperties) ->
                customizeMetricExporter(metricExporter, credentialsSupplier, configProperties))
        .addResourceCustomizer(
            (resource, configProperties) ->
                customizeResource(resource, credentialsSupplier, configProperties));
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE - 1;
  }

  /**
   * Lazily initializes and caches the {@link GoogleCredentials} used for authentication. The type
   * of the credentials is determined by the configured {@link
   * ConfigurableOption#GOOGLE_OTEL_AUTH_TOKEN_TYPE}.
   */
  private static class LazyCredentialsSupplier {
    @Nullable private OAuth2Credentials credentials;

    synchronized OAuth2Credentials get(ConfigProperties configProperties) {
      if (credentials == null) {
        credentials = createCredentials(configProperties);
      }
      return credentials;
    }
  }

  // Creates the credentials used for authentication based on the configured token type. For the
  // default token type (access_token) the Application Default Credentials are used as-is. For
  // id_token, the Application Default Credentials are wrapped into IdTokenCredentials which mint
  // Google-signed ID tokens for the configured audience.
  private static OAuth2Credentials createCredentials(ConfigProperties configProperties) {
    GoogleCredentials applicationDefaultCredentials;
    try {
      applicationDefaultCredentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_RETRIEVAL, e);
    }
    if (!isIdTokenType(configProperties)) {
      return applicationDefaultCredentials;
    }
    String audience =
        ConfigurableOption.GOOGLE_OTEL_AUTH_ID_TOKEN_AUDIENCE.getConfiguredValue(configProperties);
    if (!(applicationDefaultCredentials instanceof IdTokenProvider)) {
      throw new ConfigurationException(
          String.format(
              "GCP Authentication Extension not configured properly: the retrieved Application Default Credentials (%s) cannot mint ID tokens. Use a credential type implementing IdTokenProvider - for example the default service account of a GCP compute environment, a service account key, or impersonated credentials (gcloud auth application-default login --impersonate-service-account=<SERVICE_ACCOUNT>).",
              applicationDefaultCredentials.getClass().getSimpleName()));
    }
    return IdTokenCredentials.newBuilder()
        .setIdTokenProvider((IdTokenProvider) applicationDefaultCredentials)
        .setTargetAudience(audience)
        .build();
  }

  // Checks whether the extension is configured to attach ID tokens instead of access tokens.
  private static boolean isIdTokenType(ConfigProperties configProperties) {
    String tokenType =
        ConfigurableOption.GOOGLE_OTEL_AUTH_TOKEN_TYPE.getConfiguredValueWithFallback(
            configProperties, () -> TOKEN_TYPE_ACCESS_TOKEN);
    switch (tokenType) {
      case TOKEN_TYPE_ID_TOKEN:
        return true;
      case TOKEN_TYPE_ACCESS_TOKEN:
        return false;
      default:
        throw new ConfigurationException(
            String.format(
                "GCP Authentication Extension not configured properly: %s has unsupported value '%s'. Supported values are '%s' (default) and '%s'.",
                ConfigurableOption.GOOGLE_OTEL_AUTH_TOKEN_TYPE.getUserReadableName(),
                tokenType,
                TOKEN_TYPE_ACCESS_TOKEN,
                TOKEN_TYPE_ID_TOKEN));
    }
  }

  private static SpanExporter customizeSpanExporter(
      SpanExporter exporter,
      LazyCredentialsSupplier credentialsSupplier,
      ConfigProperties configProperties) {
    if (isSignalTargeted(SIGNAL_TYPE_TRACES, configProperties)) {
      return addAuthorizationHeaders(
          exporter, credentialsSupplier.get(configProperties), configProperties);
    } else {
      String[] params = {
        SIGNAL_TYPE_TRACES, SIGNAL_TYPE_NONE, SIGNAL_TARGET_WARNING_FIX_SUGGESTION
      };
      logger.log(
          Level.WARNING,
          "GCP Authentication Extension is not configured for signal type: {0} or is configured with signal type: {1}. {2}",
          params);
    }
    return exporter;
  }

  private static MetricExporter customizeMetricExporter(
      MetricExporter exporter,
      LazyCredentialsSupplier credentialsSupplier,
      ConfigProperties configProperties) {
    if (isSignalTargeted(SIGNAL_TYPE_METRICS, configProperties)) {
      return addAuthorizationHeaders(
          exporter, credentialsSupplier.get(configProperties), configProperties);
    } else {
      String[] params = {
        SIGNAL_TYPE_METRICS, SIGNAL_TYPE_NONE, SIGNAL_TARGET_WARNING_FIX_SUGGESTION
      };
      logger.log(
          Level.WARNING,
          "GCP Authentication Extension is not configured for signal type: {0} or is configured with signal type: {1}. {2}",
          params);
    }
    return exporter;
  }

  // Checks if the auth extension is configured to target the passed signal for authentication.
  private static boolean isSignalTargeted(String checkSignal, ConfigProperties configProperties) {
    String userSpecifiedTargetedSignals =
        ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getConfiguredValueWithFallback(
            configProperties, () -> SIGNAL_TYPE_ALL);
    List<String> targetedSignals =
        stream(userSpecifiedTargetedSignals.split(",")).map(String::trim).collect(toList());
    if (targetedSignals.contains(SIGNAL_TYPE_NONE)) {
      return false;
    }
    return targetedSignals.contains(checkSignal) || targetedSignals.contains(SIGNAL_TYPE_ALL);
  }

  // Adds authorization headers to the calls made by the OtlpGrpcSpanExporter and
  // OtlpHttpSpanExporter.
  private static SpanExporter addAuthorizationHeaders(
      SpanExporter exporter, OAuth2Credentials credentials, ConfigProperties configProperties) {
    if (exporter instanceof OtlpHttpSpanExporter) {
      OtlpHttpSpanExporterBuilder builder =
          ((OtlpHttpSpanExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials, configProperties));
      return builder.build();
    } else if (exporter instanceof OtlpGrpcSpanExporter) {
      OtlpGrpcSpanExporterBuilder builder =
          ((OtlpGrpcSpanExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials, configProperties));
      return builder.build();
    }
    return exporter;
  }

  // Adds authorization headers to the calls made by the OtlpGrpcMetricExporter and
  // OtlpHttpMetricExporter.
  private static MetricExporter addAuthorizationHeaders(
      MetricExporter exporter, OAuth2Credentials credentials, ConfigProperties configProperties) {
    if (exporter instanceof OtlpHttpMetricExporter) {
      OtlpHttpMetricExporterBuilder builder =
          ((OtlpHttpMetricExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials, configProperties));
      return builder.build();
    } else if (exporter instanceof OtlpGrpcMetricExporter) {
      OtlpGrpcMetricExporterBuilder builder =
          ((OtlpGrpcMetricExporter) exporter)
              .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials, configProperties));
      return builder.build();
    }
    return exporter;
  }

  private static Map<String, String> getRequiredHeaderMap(
      OAuth2Credentials credentials, ConfigProperties configProperties) {
    Map<String, List<String>> gcpHeaders;
    try {
      // this also refreshes the credentials, if required
      gcpHeaders = credentials.getRequestMetadata();
    } catch (IOException e) {
      throw new GoogleAuthException(Reason.FAILED_ADC_REFRESH, e);
    }
    Map<String, String> flattenedHeaders =
        gcpHeaders.entrySet().stream()
            .collect(
                toMap(
                    Map.Entry::getKey,
                    entry ->
                        entry.getValue().stream()
                            .filter(Objects::nonNull) // Filter nulls
                            .filter(s -> !s.isEmpty()) // Filter empty strings
                            .collect(joining(","))));
    // Add quota user project header if not detected by the auth library and user provided it via
    // system properties. The quota project concept only applies to requests against Google Cloud
    // APIs authenticated with access tokens, so it is skipped when ID tokens are used.
    if (!isIdTokenType(configProperties)
        && !flattenedHeaders.containsKey(QUOTA_USER_PROJECT_HEADER)) {
      Optional<String> maybeConfiguredQuotaProjectId =
          ConfigurableOption.GOOGLE_CLOUD_QUOTA_PROJECT.getConfiguredValueAsOptional(
              configProperties);
      maybeConfiguredQuotaProjectId.ifPresent(
          configuredQuotaProjectId ->
              flattenedHeaders.put(QUOTA_USER_PROJECT_HEADER, configuredQuotaProjectId));
    }
    return flattenedHeaders;
  }

  // Updates the current resource with the attributes required for ingesting OTLP data on GCP.
  private static Resource customizeResource(
      Resource resource,
      LazyCredentialsSupplier credentialsSupplier,
      ConfigProperties configProperties) {
    if (!isSignalTargeted(SIGNAL_TYPE_TRACES, configProperties)
        && !isSignalTargeted(SIGNAL_TYPE_METRICS, configProperties)) {
      return resource;
    }
    // The gcp.project_id resource attribute is only required when ingesting telemetry to the GCP
    // Telemetry API with access tokens. It is not required for ID token authenticated exports,
    // which target arbitrary IAM-protected OTLP endpoints.
    if (isIdTokenType(configProperties)) {
      return resource;
    }
    String gcpProjectId;
    try {
      gcpProjectId = ConfigurableOption.GOOGLE_CLOUD_PROJECT.getConfiguredValue(configProperties);
    } catch (ConfigurationException e) {
      // This line is only reachable for the access_token type, for which the supplier always
      // returns the GoogleCredentials retrieved as Application Default Credentials.
      gcpProjectId = ((GoogleCredentials) credentialsSupplier.get(configProperties)).getProjectId();
      if (gcpProjectId == null || gcpProjectId.isEmpty()) {
        throw e;
      }
    }
    Resource res = Resource.create(Attributes.of(stringKey(GCP_USER_PROJECT_ID_KEY), gcpProjectId));
    return resource.merge(res);
  }
}
