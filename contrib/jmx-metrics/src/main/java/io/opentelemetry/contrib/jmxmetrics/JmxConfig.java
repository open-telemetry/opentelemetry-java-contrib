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

class JmxConfig {
  static final String PREFIX = "otel.";
  static final String SERVICE_URL = PREFIX + "jmx.service.url";
  static final String GROOVY_SCRIPT = PREFIX + "jmx.groovy.script";
  static final String INTERVAL_MILLISECONDS = PREFIX + "jmx.interval.milliseconds";
  static final String EXPORTER_TYPE = PREFIX + "exporter";

  static final String OTLP_ENDPOINT = PREFIX + "otlp.endpoint";

  static final String PROMETHEUS_HOST = PREFIX + "prometheus.host";
  static final String PROMETHEUS_PORT = PREFIX + "prometheus.port";

  static final String JMX_USERNAME = PREFIX + "jmx.username";
  static final String JMX_PASSWORD = PREFIX + "jmx.password";
  static final String JMX_REMOTE_PROFILE = PREFIX + "jmx.remote.profile";
  static final String JMX_REALM = PREFIX + "jmx.realm";

  final String serviceUrl;
  final String groovyScript;
  final int intervalMilliseconds;
  final String exporterType;

  final String otlpExporterEndpoint;

  final String prometheusExporterHost;
  final int prometheusExporterPort;

  final String username;
  final String password;
  final String realm;
  final String remoteProfile;

  final Properties properties;

  JmxConfig(final Properties props) {
    properties = new Properties(props);
    // command line takes precedence
    properties.putAll(System.getProperties());

    serviceUrl = properties.getProperty(SERVICE_URL);
    groovyScript = properties.getProperty(GROOVY_SCRIPT);

    int interval = getProperty(INTERVAL_MILLISECONDS, 10000);
    intervalMilliseconds = interval == 0 ? 10000 : interval;

    exporterType = properties.getProperty(EXPORTER_TYPE, "logging");

    otlpExporterEndpoint = properties.getProperty(OTLP_ENDPOINT);

    prometheusExporterHost = properties.getProperty(PROMETHEUS_HOST, "localhost");
    prometheusExporterPort = getProperty(PROMETHEUS_PORT, 9090);

    username = properties.getProperty(JMX_USERNAME);
    password = properties.getProperty(JMX_PASSWORD);
    remoteProfile = properties.getProperty(JMX_REMOTE_PROFILE);
    realm = properties.getProperty(JMX_REALM);
  }

  JmxConfig() {
    this(new Properties());
  }

  private int getProperty(final String key, final int dfault) {
    String propVal = properties.getProperty(key);
    if (propVal == null) {
      return dfault;
    }
    try {
      return Integer.parseInt(propVal);
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Failed to parse " + key, e);
    }
  }

  /** Will determine if parsed config is complete, setting any applicable defaults. */
  void validate() {
    if (isBlank(serviceUrl)) {
      throw new ConfigurationException(SERVICE_URL + " must be specified.");
    }

    if (isBlank(groovyScript)) {
      throw new ConfigurationException(GROOVY_SCRIPT + " must be specified.");
    }

    if (isBlank(otlpExporterEndpoint)
        && (!isBlank(exporterType) && exporterType.equalsIgnoreCase("otlp"))) {
      throw new ConfigurationException(OTLP_ENDPOINT + " must be specified for otlp format.");
    }

    if (intervalMilliseconds < 0) {
      throw new ConfigurationException(INTERVAL_MILLISECONDS + " must be positive.");
    }
  }

  /**
   * Determines if a String is null or without non-whitespace chars.
   *
   * @param s - {@link String} to evaluate
   * @return - if s is null or without non-whitespace chars.
   */
  static boolean isBlank(final String s) {
    if (s == null) {
      return true;
    }
    return s.trim().length() == 0;
  }
}
