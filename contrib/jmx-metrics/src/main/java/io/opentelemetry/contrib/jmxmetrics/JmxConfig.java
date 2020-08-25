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
  protected static final String PREFIX = "otel.";
  protected static final String SERVICE_URL = PREFIX + "jmx.service.url";
  protected static final String GROOVY_SCRIPT = PREFIX + "jmx.groovy.script";
  protected static final String INTERVAL_MILLISECONDS = PREFIX + "jmx.interval.milliseconds";
  protected static final String EXPORTER_TYPE = PREFIX + "exporter";

  protected static final String OTLP_ENDPOINT = PREFIX + "otlp.endpoint";

  protected static final String PROMETHEUS_HOST = PREFIX + "prometheus.host";
  protected static final String PROMETHEUS_PORT = PREFIX + "prometheus.port";

  protected static final String JMX_USERNAME = PREFIX + "jmx.username";
  protected static final String JMX_PASSWORD = PREFIX + "jmx.password";
  protected static final String JMX_REMOTE_PROFILES = PREFIX + "jmx.remote.profiles";
  protected static final String JMX_REALM = PREFIX + "jmx.realm";

  public final String serviceUrl;
  public final String groovyScript;
  public final int intervalMilliseconds;
  public final String exporterType;

  public final String otlpExporterEndpoint;

  public final String prometheusExporterHost;
  public final int prometheusExporterPort;

  public final String username;
  public final String password;
  public final String realm;
  public final String remoteProfiles;

  public final Properties properties;

  public JmxConfig(final Properties props) {
    this.properties = new Properties(props);
    // command line takes precedence
    this.properties.putAll(System.getProperties());

    serviceUrl = getProperty(SERVICE_URL, "");
    groovyScript = getProperty(GROOVY_SCRIPT, "");

    final int interval = getProperty(INTERVAL_MILLISECONDS, 10000);
    intervalMilliseconds = interval == 0 ? 10000 : interval;

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
    final String propVal = properties.getProperty(key);
    return (propVal == null) ? dfault : propVal;
  }

  private int getProperty(final String key, final int dfault) {
    final String propVal = properties.getProperty(key);
    if (propVal == null) {
      return dfault;
    }
    try {
      return Integer.parseInt(propVal);
    } catch (NumberFormatException e) {
      throw new ConfigureError("Failed to parse " + key, e);
    }
  }

  /**
   * Will determine if parsed config is complete, setting any applicable defaults.
   *
   * @throws ConfigureError - Thrown if a configuration value is missing or invalid.
   */
  public void validate() throws ConfigureError {
    if (isBlank(this.serviceUrl)) {
      throw new ConfigureError(SERVICE_URL + " must be specified.");
    }

    if (isBlank(this.groovyScript)) {
      throw new ConfigureError(GROOVY_SCRIPT + " must be specified.");
    }

    if (isBlank(this.otlpExporterEndpoint) && this.exporterType.equalsIgnoreCase("otlp")) {
      throw new ConfigureError(OTLP_ENDPOINT + " must be specified for otlp format.");
    }

    if (this.intervalMilliseconds < 0) {
      throw new ConfigureError(INTERVAL_MILLISECONDS + " must be positive.");
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
