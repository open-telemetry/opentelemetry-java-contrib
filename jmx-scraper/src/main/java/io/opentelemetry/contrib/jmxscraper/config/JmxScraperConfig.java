/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import java.util.Collections;
import java.util.Set;

/** This class keeps application settings */
public class JmxScraperConfig {
  String serviceUrl = "";
  String customJmxScrapingConfigPath = "";
  Set<String> targetSystems = Collections.emptySet();
  int intervalMilliseconds;
  String metricsExporterType = "";

  String otlpExporterEndpoint = "";

  String username = "";
  String password = "";
  String realm = "";
  String remoteProfile = "";
  boolean registrySsl;

  JmxScraperConfig() {}

  public String getServiceUrl() {
    return serviceUrl;
  }

  public String getCustomJmxScrapingConfigPath() {
    return customJmxScrapingConfigPath;
  }

  public Set<String> getTargetSystems() {
    return targetSystems;
  }

  public int getIntervalMilliseconds() {
    return intervalMilliseconds;
  }

  public String getMetricsExporterType() {
    return metricsExporterType;
  }

  public String getOtlpExporterEndpoint() {
    return otlpExporterEndpoint;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getRealm() {
    return realm;
  }

  public String getRemoteProfile() {
    return remoteProfile;
  }

  public boolean isRegistrySsl() {
    return registrySsl;
  }
}
