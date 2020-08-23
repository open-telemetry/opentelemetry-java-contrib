/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.jmxmetrics;

import java.util.Properties;

public class JmxConfig {
  public String serviceUrl;
  public String groovyScript;

  public int intervalMilliseconds;
  public String exporterType;
  public String otlpExporterEndpoint;

  public String prometheusExporterHost;
  public int prometheusExporterPort;

  public String username;
  public String password;

  public String keyStorePath;
  public String keyStorePassword;
  public String keyStoreType;
  public String trustStorePath;
  public String trustStorePassword;
  public String jmxRemoteProfiles;
  public String realm;

  private Properties properties;
  private static final String prefix = "otel.jmx.metrics.";

  public JmxConfig(final Properties properties) {
    this.properties = new Properties(properties);
    // command line takes precedence
    this.properties.putAll(System.getProperties());

    serviceUrl = getProperty("service.url", null);
    groovyScript = getProperty("groovy.script", null);
    try {
      intervalMilliseconds = Integer.parseInt(getProperty("interval.milliseconds", "10000"));
    } catch (NumberFormatException e) {
      throw new ConfigureError("Failed to parse " + prefix + "interval.milliseconds", e);
    }
    exporterType = getProperty("exporter.type", "logging");
    otlpExporterEndpoint = getProperty("exporter.otlp.endpoint", null);

    prometheusExporterHost = getProperty("exporter.prometheus.host", "localhost");
    try {
      prometheusExporterPort = Integer.parseInt(getProperty("exporter.prometheus.port", "9090"));
    } catch (NumberFormatException e) {
      throw new ConfigureError("Failed to parse " + prefix + "exporter.prometheus.port", e);
    }

    username = getProperty("username", null);
    password = getProperty("password", null);
    keyStorePath = getProperty("keystore.path", null);
    keyStorePassword = getProperty("keystore.password", null);
    keyStoreType = getProperty("keystore.type", null);
    trustStorePath = getProperty("truststore.path", null);
    trustStorePassword = getProperty("truststore.password", null);
    jmxRemoteProfiles = getProperty("remote.profiles", null);
    realm = getProperty("realm", null);
  }

  public JmxConfig() {
    this(new Properties());
  }

  private String getProperty(final String key, final String dfault) {
    return properties.getProperty(prefix + key, dfault);
  }

  /**
   * Will determine if parsed config is complete, setting any applicable defaults.
   *
   * @throws ConfigureError - Thrown if a configuration value is missing or invalid.
   */
  public void validate() throws ConfigureError {
    if (isBlank(this.serviceUrl)) {
      throw new ConfigureError(prefix + "service.url must be specified.");
    }

    if (isBlank(this.groovyScript)) {
      throw new ConfigureError(prefix + "groovy.script must be specified.");
    }

    if (isBlank(this.otlpExporterEndpoint) && this.exporterType.equalsIgnoreCase("otlp")) {
      throw new ConfigureError(prefix + "exporter.endpoint must be specified for otlp format.");
    }

    if (this.intervalMilliseconds < 0) {
      throw new ConfigureError(prefix + "interval.milliseconds must be positive.");
    }

    if (this.intervalMilliseconds == 0) {
      this.intervalMilliseconds = 10;
    }
  }

  /**
   * Determines if a String is null or without non-whitespace chars.
   *
   * @param s - {@link String} to evaluate
   * @return - if s is null or without non-whitespace chars.
   */
  public static boolean isBlank(final String s) {
    if (s == null) {
      return true;
    }
    return s.trim().length() == 0;
  }
}
