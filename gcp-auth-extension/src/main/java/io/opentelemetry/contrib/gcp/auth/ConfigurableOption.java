/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.util.Locale;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/**
 * An enum representing configurable options for a GCP Authentication Extension. Each option has a
 * user-readable name and can be configured using environment variables or system properties.
 */
enum ConfigurableOption {
  /**
   * Represents the Google Cloud Project ID option. Can be configured using the environment variable
   * `GOOGLE_CLOUD_PROJECT` or the system property `google.cloud.project`.
   */
  GOOGLE_CLOUD_PROJECT("Google Cloud Project ID"),

  /**
   * Represents the Google Cloud Quota Project ID option. Can be configured using the environment
   * variable `GOOGLE_CLOUD_QUOTA_PROJECT` or the system property `google.cloud.quota.project`. The
   * quota project is the project that is used for quota management and billing for the API usage.
   *
   * <p>The environment variable name is selected to be consistent with the <a
   * href="https://cloud.google.com/docs/quotas/set-quota-project">official GCP client
   * libraries</a>.
   */
  GOOGLE_CLOUD_QUOTA_PROJECT("Google Cloud Quota Project ID"),

  /**
   * Specifies a comma-separated list of OpenTelemetry signals for which this authentication
   * extension should be active. The authentication mechanisms provided by this extension will only
   * be applied to the listed signals. If not set, {@code all} is assumed to be set which means
   * authentication is enabled for all supported signals.
   *
   * <p>Valid signal values are:
   *
   * <ul>
   *   <li>{@code metrics} - Enables authentication for metric exports.
   *   <li>{@code traces} - Enables authentication for trace exports.
   *   <li>{@code all} - Enables authentication for all exports.
   * </ul>
   *
   * <p>The values are case-sensitive. Whitespace around commas and values is ignored. Can be
   * configured using the environment variable `GOOGLE_OTEL_AUTH_TARGET_SIGNALS` or the system
   * property `google.otel.auth.target.signals`.
   */
  GOOGLE_OTEL_AUTH_TARGET_SIGNALS("Target Signals for Google Authentication Extension");

  private final String userReadableName;
  private final String environmentVariableName;
  private final String systemPropertyName;

  ConfigurableOption(String userReadableName) {
    this.userReadableName = userReadableName;
    this.environmentVariableName = this.name();
    this.systemPropertyName =
        this.environmentVariableName.toLowerCase(Locale.ROOT).replace('_', '.');
  }

  /**
   * Returns the environment variable name associated with this option.
   *
   * @return the environment variable name (e.g., GOOGLE_CLOUD_PROJECT)
   */
  String getEnvironmentVariable() {
    return this.environmentVariableName;
  }

  /**
   * Returns the system property name associated with this option.
   *
   * @return the system property name (e.g., google.cloud.project)
   */
  String getSystemProperty() {
    return this.systemPropertyName;
  }

  /**
   * Returns the user readable name associated with this option.
   *
   * @return the user readable name (e.g., "Google Cloud Quota Project ID")
   */
  String getUserReadableName() {
    return this.userReadableName;
  }

  /**
   * Retrieves the configured value for this option. This method checks the environment variable
   * first and then the system property.
   *
   * @return The configured value as a string, or throws an exception if not configured.
   * @throws ConfigurationException if neither the environment variable nor the system property is
   *     set.
   */
  <T> T getRequiredConfiguredValue(
      ConfigProperties configProperties, BiFunction<ConfigProperties, String, T> extractor) {
    T configuredValue = getConfiguredValue(configProperties, extractor);
    if (configuredValue == null) {
      throw new ConfigurationException(
          String.format(
              "GCP Authentication Extension not configured properly: %s not configured. "
                  + "Configure it by exporting environment variable %s or system property %s",
              this.userReadableName, this.getEnvironmentVariable(), this.getSystemProperty()));
    }
    return configuredValue;
  }

  /**
   * Retrieves the configured value for this option. This method checks the environment variable
   * first and then the system property.
   *
   * @return The configured value as a string, or throws an exception if not configured.
   */
  @Nullable
  <T> T getConfiguredValue(
      ConfigProperties configProperties, BiFunction<ConfigProperties, String, T> extractor) {
    T configuredValue = extractor.apply(configProperties, this.getSystemProperty());
    if (configuredValue instanceof String) {
      String value = (String) configuredValue;
      if (value.isEmpty()) {
        configuredValue = null; // Treat empty string as not configured
      }
    }

    return configuredValue;
  }
}
