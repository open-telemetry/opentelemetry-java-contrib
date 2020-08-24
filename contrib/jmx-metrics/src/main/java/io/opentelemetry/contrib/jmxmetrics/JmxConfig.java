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
  private static final String PREFIX = "otel.";
  private static final String SERVICE_URL = "jmx.service.url";
  private static final String GROOVY_SCRIPT = "jmx.groovy.script";
  private static final String INTERVAL_MILLISECONDS = "jmx.interval.milliseconds";
  private static final String EXPORTER_TYPE = "exporter";

  private static final String OTLP_ENDPOINT = "otlp.endpoint";

  private static final String PROMETHEUS_HOST = "prometheus.host";
  private static final String PROMETHEUS_PORT = "prometheus.port";

  private static final String JMX_USERNAME = "jmx.username";
  private static final String JMX_PASSWORD = "jmx.password";
  private static final String JMX_REMOTE_PROFILES = "jmx.remote.profiles";
  private static final String JMX_REALM = "jmx.realm";

  public String serviceUrl;
  public String groovyScript;
  public int intervalMilliseconds;
  public String exporterType;

  public String otlpExporterEndpoint;

  public String prometheusExporterHost;
  public int prometheusExporterPort;

  public String username;
  public String password;
  public String realm;
  public String remoteProfiles;

  public Properties properties;

  public JmxConfig(final Properties properties) {
    this.properties = new Properties(properties);
    // command line takes precedence
    this.properties.putAll(System.getProperties());

    serviceUrl = getProperty(SERVICE_URL, "");
    groovyScript = getProperty(GROOVY_SCRIPT, "");
    intervalMilliseconds = getProperty(INTERVAL_MILLISECONDS, 10000);
    exporterType = getProperty(EXPORTER_TYPE, "logging");

    otlpExporterEndpoint = getProperty(OTLP_ENDPOINT, "");

    prometheusExporterHost = getProperty(PROMETHEUS_HOST, "localhost");
    prometheusExporterPort = getProperty(PROMETHEUS_PORT, 9090);

    username = getProperty(JMX_USERNAME, "");
    password = getProperty(JMX_PASSWORD, "");
    remoteProfiles = getProperty(JMX_REMOTE_PROFILES, "");
    realm = getProperty(JMX_REALM, "");
  }

  public JmxConfig() {
    this(new Properties());
  }

  private String getProperty(final String key, final String dfault) {
    final String propVal = properties.getProperty(PREFIX + key);
    return (propVal == null) ? dfault : propVal;
  }

  private int getProperty(final String key, final int dfault) {
    final String propVal = properties.getProperty(PREFIX + key);
    if (propVal == null) {
      return dfault;
    }
    try {
      return Integer.parseInt(propVal);
    } catch (NumberFormatException e) {
      throw new ConfigureError("Failed to parse " + PREFIX + key, e);
    }
  }

  /**
   * Will determine if parsed config is complete, setting any applicable defaults.
   *
   * @throws ConfigureError - Thrown if a configuration value is missing or invalid.
   */
  public void validate() throws ConfigureError {
    if (isBlank(this.serviceUrl)) {
      throw new ConfigureError(PREFIX + SERVICE_URL + " must be specified.");
    }

    if (isBlank(this.groovyScript)) {
      throw new ConfigureError(PREFIX + GROOVY_SCRIPT + " must be specified.");
    }

    if (isBlank(this.otlpExporterEndpoint) && this.exporterType.equalsIgnoreCase("otlp")) {
      throw new ConfigureError(PREFIX + OTLP_ENDPOINT + " must be specified for otlp format.");
    }

    if (this.intervalMilliseconds < 0) {
      throw new ConfigureError(PREFIX + INTERVAL_MILLISECONDS + " must be positive.");
    }

    if (this.intervalMilliseconds == 0) {
      this.intervalMilliseconds = 10000;
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
