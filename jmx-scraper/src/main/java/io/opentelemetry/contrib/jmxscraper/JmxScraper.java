/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.contrib.jmxscraper.config.PropertiesCustomizer;
import io.opentelemetry.contrib.jmxscraper.config.PropertiesSupplier;
import io.opentelemetry.instrumentation.jmx.engine.JmxMetricInsight;
import io.opentelemetry.instrumentation.jmx.engine.MetricConfiguration;
import io.opentelemetry.instrumentation.jmx.yaml.RuleParser;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class JmxScraper {
  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());
  private static final String CONFIG_ARG = "-config";

  private final JmxConnectorBuilder client;
  private final JmxMetricInsight service;
  private final JmxScraperConfig config;

  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Main method to create and run a {@link JmxScraper} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  @SuppressWarnings({"SystemOut", "SystemExitOutsideMain"})
  public static void main(String[] args) {

    try {
      Properties argsConfig = parseArgs(Arrays.asList(args));
      propagateToSystemProperties(argsConfig);

      // auto-configure and register SDK
      PropertiesCustomizer configCustomizer = new PropertiesCustomizer();
      AutoConfiguredOpenTelemetrySdk.builder()
          .addPropertiesSupplier(new PropertiesSupplier(argsConfig))
          .addPropertiesCustomizer(configCustomizer)
          .build();

      JmxScraperConfig scraperConfig = configCustomizer.getScraperConfig();

      JmxMetricInsight service =
          JmxMetricInsight.createService(
              GlobalOpenTelemetry.get(), scraperConfig.getSamplingInterval().toMillis());
      JmxConnectorBuilder connectorBuilder =
          JmxConnectorBuilder.createNew(scraperConfig.getServiceUrl());

      Optional.ofNullable(scraperConfig.getUsername()).ifPresent(connectorBuilder::withUser);
      Optional.ofNullable(scraperConfig.getPassword()).ifPresent(connectorBuilder::withPassword);

      JmxScraper jmxScraper = new JmxScraper(connectorBuilder, service, scraperConfig);
      jmxScraper.start();
    } catch (ConfigurationException e) {
      System.err.println("ERROR: invalid configuration " + e.getMessage());
      System.exit(1);
    } catch (ArgumentsParsingException e) {
      System.err.println(
          "Usage: java -jar <path_to_jmxscraper.jar> "
              + "-config <path_to_config.properties or - for stdin>");
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Unable to connect " + e.getMessage());
      System.exit(2);
    } catch (RuntimeException e) {
      e.printStackTrace(System.err);
      System.exit(3);
    }
  }

  // package private for testing
  static void propagateToSystemProperties(Properties properties) {
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();
      if (key.startsWith("javax.net.ssl.keyStore") || key.startsWith("javax.net.ssl.trustStore")) {
        if (System.getProperty(key) == null) {
          System.setProperty(key, value);
        }
      }
    }
  }

  /**
   * Create {@link Properties} from command line options
   *
   * @param args application commandline arguments
   */
  static Properties parseArgs(List<String> args) throws ArgumentsParsingException {

    if (args.isEmpty()) {
      // empty properties from stdin or external file
      // config could still be provided through JVM system properties
      return new Properties();
    }
    if (args.size() != 2) {
      throw new ArgumentsParsingException("Exactly two arguments expected, got " + args.size());
    }
    if (!args.get(0).equalsIgnoreCase(CONFIG_ARG)) {
      throw new ArgumentsParsingException("Unexpected first argument must be '" + CONFIG_ARG + "'");
    }

    String path = args.get(1);
    if (path.trim().equals("-")) {
      return loadPropertiesFromStdin();
    } else {
      return loadPropertiesFromPath(path);
    }
  }

  private static Properties loadPropertiesFromStdin() throws ArgumentsParsingException {
    Properties properties = new Properties();
    try (InputStream is = new DataInputStream(System.in)) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new ArgumentsParsingException("Failed to read config properties from stdin", e);
    }
  }

  private static Properties loadPropertiesFromPath(String path) throws ArgumentsParsingException {
    Properties properties = new Properties();
    try (InputStream is = Files.newInputStream(Paths.get(path))) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new ArgumentsParsingException(
          "Failed to read config properties file: '" + path + "'", e);
    }
  }

  JmxScraper(JmxConnectorBuilder client, JmxMetricInsight service, JmxScraperConfig config) {
    this.client = client;
    this.service = service;
    this.config = config;
  }

  private void start() throws IOException {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("JMX scraping stopped");
                  running.set(false);
                }));

    try (JMXConnector connector = client.build()) {
      MBeanServerConnection connection = connector.getMBeanServerConnection();
      service.startRemote(getMetricConfig(config), () -> Collections.singletonList(connection));

      running.set(true);
      logger.info("JMX scraping started");

      while (running.get()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // silently ignored
        }
      }
    }
  }

  private static MetricConfiguration getMetricConfig(JmxScraperConfig scraperConfig) {
    MetricConfiguration config = new MetricConfiguration();
    for (String system : scraperConfig.getTargetSystems()) {
      addRulesForSystem(system, config);
    }
    // TODO : add ability for user to provide custom yaml configurations
    return config;
  }

  private static void addRulesForSystem(String system, MetricConfiguration conf) {
    String yamlResource = system + ".yaml";
    try (InputStream inputStream =
        JmxScraper.class.getClassLoader().getResourceAsStream(yamlResource)) {
      if (inputStream != null) {
        RuleParser parserInstance = RuleParser.get();
        parserInstance.addMetricDefsTo(conf, inputStream, system);
      } else {
        throw new IllegalArgumentException("No support for system" + system);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while loading rules for system " + system, e);
    }
  }
}
