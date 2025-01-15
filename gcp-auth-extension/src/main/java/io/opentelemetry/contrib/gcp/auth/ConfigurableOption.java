/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * An enum representing configurable options for a GCP Authentication Extension. Each option has a
 * user-readable name and can be configured using environment variables or system properties.
 */
public enum ConfigurableOption {
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
  GOOGLE_CLOUD_QUOTA_PROJECT("Google Cloud Quota Project ID");

  private final String userReadableName;
  private final String environmentVariableName;
  private final String systemPropertyName;

  ConfigurableOption(String userReadableName) {
    this.userReadableName = userReadableName;
    this.environmentVariableName = this.name();
    this.systemPropertyName =
        this.environmentVariableName.toLowerCase(Locale.ENGLISH).replace('_', '.');
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
   * Retrieves the configured value for this option. This method checks the environment variable
   * first and then the system property.
   *
   * @return The configured value as a string, or throws an exception if not configured.
   * @throws ConfigurationException if neither the environment variable nor the system property is
   *     set.
   */
  String getConfiguredValue() {
    String envVar = System.getenv(this.getEnvironmentVariable());
    String sysProp = System.getProperty(this.getSystemProperty());

    if (envVar != null && !envVar.isEmpty()) {
      return envVar;
    } else if (sysProp != null && !sysProp.isEmpty()) {
      return sysProp;
    } else {
      throw new ConfigurationException(
          String.format(
              "GCP Authentication Extension not configured properly: %s not configured. Configure it by exporting environment variable %s or system property %s",
              this.userReadableName, this.getEnvironmentVariable(), this.getSystemProperty()));
    }
  }

  /**
   * Retrieves the value for this option, prioritizing environment variables and system properties.
   * If neither an environment variable nor a system property is set for this option, the provided
   * fallback function is used to determine the value.
   *
   * @param fallback A {@link Supplier} that provides the default value for the option when it is
   *     not explicitly configured via an environment variable or system property.
   * @return The configured value for the option, obtained from the environment variable, system
   *     property, or the fallback function, in that order of precedence.
   */
  String getConfiguredValueWithFallback(Supplier<String> fallback) {
    try {
      return this.getConfiguredValue();
    } catch (ConfigurationException e) {
      return fallback.get();
    }
  }
}
