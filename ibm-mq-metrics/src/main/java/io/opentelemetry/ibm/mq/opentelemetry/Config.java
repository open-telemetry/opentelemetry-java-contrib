/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities reading configuration and create domain objects */
final class Config {

  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  private Config() {}

  static void setUpSslConnection(Map<String, ?> config) {
    getConfigValueAndSetSystemProperty(config, "keyStorePath", "javax.net.ssl.keyStore");
    getConfigValueAndSetSystemProperty(
        config, "keyStorePassword", "javax.net.ssl.keyStorePassword");
    getConfigValueAndSetSystemProperty(config, "trustStorePath", "javax.net.ssl.trustStorePath");
    getConfigValueAndSetSystemProperty(
        config, "trustStorePassword", "javax.net.ssl.trustStorePassword");
  }

  private static void getConfigValueAndSetSystemProperty(
      Map<String, ?> otlpConfig, String configKey, String systemKey) {
    Object configValue = otlpConfig.get(configKey);
    if (configValue instanceof String && !((String) configValue).trim().isEmpty()) {
      System.setProperty(systemKey, (String) configValue);
    }
  }

  static void configureSecurity(ConfigWrapper config) {
    Map<String, String> sslConnection = config.getSslConnection();
    if (sslConnection.isEmpty()) {
      logger.debug(
          "ssl truststore and keystore are not configured in config.yml, if SSL is enabled, pass them as jvm args");
      return;
    }

    configureTrustStore(sslConnection);
    configureKeyStore(sslConnection);
  }

  private static void configureTrustStore(Map<String, String> sslConnection) {
    String trustStorePath = sslConnection.get("trustStorePath");
    if (trustStorePath == null || trustStorePath.isEmpty()) {
      logger.debug(
          "trustStorePath is not set in config.yml, ignoring setting trustStorePath as system property");
      return;
    }

    System.setProperty("javax.net.ssl.trustStore", trustStorePath);
    logger.debug("System property set for javax.net.ssl.trustStore is {}", trustStorePath);

    String trustStorePassword = sslConnection.get("trustStorePassword");

    if (trustStorePassword != null && !trustStorePassword.isEmpty()) {
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
      logger.debug("System property set for javax.net.ssl.trustStorePassword is xxxxx");
    }
  }

  private static void configureKeyStore(Map<String, String> sslConnection) {
    String keyStorePath = sslConnection.get("keyStorePath");
    if (keyStorePath == null || keyStorePath.isEmpty()) {
      logger.debug(
          "keyStorePath is not set in config.yml, ignoring setting keyStorePath as system property");
      return;
    }

    System.setProperty("javax.net.ssl.keyStore", keyStorePath);
    logger.debug("System property set for javax.net.ssl.keyStore is {}", keyStorePath);
    String keyStorePassword = sslConnection.get("keyStorePassword");
    if (keyStorePassword != null && !keyStorePassword.isEmpty()) {
      System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
      logger.debug("System property set for javax.net.ssl.keyStorePassword is xxxxx");
    }
  }
}
