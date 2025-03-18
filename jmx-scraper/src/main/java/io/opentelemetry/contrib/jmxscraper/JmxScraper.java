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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class JmxScraper {
  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());
  private static final String CONFIG_ARG = "-config";
  private static final String TEST_ARG = "-test";

  private final JmxConnectorBuilder client;
  private final JmxMetricInsight service;
  private final JmxScraperConfig config;

  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Main method to create and run a {@link JmxScraper} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  @SuppressWarnings("SystemExitOutsideMain")
  public static void main(String[] args) {

    // set log format
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%n");

    List<String> effectiveArgs = new ArrayList<>(Arrays.asList(args));
    boolean testMode = effectiveArgs.remove(TEST_ARG);

    try {
      Properties argsConfig = argsToConfig(effectiveArgs);
      propagateToSystemProperties(argsConfig);

      // auto-configure and register SDK
      PropertiesCustomizer configCustomizer = new PropertiesCustomizer();
      AutoConfiguredOpenTelemetrySdk.builder()
          .addPropertiesSupplier(new PropertiesSupplier(argsConfig))
          .addPropertiesCustomizer(configCustomizer)
          .setResultAsGlobal()
          .build();

      JmxScraperConfig scraperConfig = configCustomizer.getScraperConfig();

      long exportSeconds = scraperConfig.getSamplingInterval().toMillis() / 1000;
      logger.log(Level.INFO, "metrics export interval (seconds) =  " + exportSeconds);

      JmxMetricInsight service =
          JmxMetricInsight.createService(
              GlobalOpenTelemetry.get(), scraperConfig.getSamplingInterval().toMillis());
      JmxConnectorBuilder connectorBuilder =
          JmxConnectorBuilder.createNew(scraperConfig.getServiceUrl());

      Optional.ofNullable(scraperConfig.getUsername()).ifPresent(connectorBuilder::withUser);
      Optional.ofNullable(scraperConfig.getPassword()).ifPresent(connectorBuilder::withPassword);

      if (scraperConfig.isRegistrySsl()) {
        connectorBuilder.withSslRegistry();
      }

      if (testMode) {
        System.exit(testConnection(connectorBuilder) ? 0 : 1);
      } else {
        JmxScraper jmxScraper = new JmxScraper(connectorBuilder, service, scraperConfig);
        jmxScraper.start();
      }
    } catch (ConfigurationException e) {
      logger.log(Level.SEVERE, "invalid configuration ", e);
      System.exit(1);
    } catch (InvalidArgumentException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      logger.info("Usage: java -jar <path_to_jmxscraper.jar> [-test] [-config <conf>]");
      logger.info("  -test           test JMX connection with provided configuration and exit");
      logger.info(
          "  -config <conf>  provide configuration, where <conf> is - for stdin, or <path_to_config.properties>");
      System.exit(1);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Unable to connect ", e);
      System.exit(2);
    } catch (RuntimeException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      System.exit(3);
    }
  }

  private static boolean testConnection(JmxConnectorBuilder connectorBuilder) {
    try (JMXConnector connector = connectorBuilder.build()) {

      MBeanServerConnection connection = connector.getMBeanServerConnection();
      Integer mbeanCount = connection.getMBeanCount();
      if (mbeanCount > 0) {
        logger.log(Level.INFO, "JMX connection test OK");
        return true;
      } else {
        logger.log(Level.SEVERE, "JMX connection test ERROR");
        return false;
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "JMX connection test ERROR", e);
      return false;
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
  static Properties argsToConfig(List<String> args) throws InvalidArgumentException {

    if (args.isEmpty()) {
      // empty properties from stdin or external file
      // config could still be provided through JVM system properties
      return new Properties();
    }
    if (args.size() != 2) {
      throw new InvalidArgumentException("Unexpected number of arguments");
    }
    if (!args.get(0).equalsIgnoreCase(CONFIG_ARG)) {
      throw new InvalidArgumentException("Unexpected argument must be '" + CONFIG_ARG + "'");
    }

    String path = args.get(1);
    if (path.trim().equals("-")) {
      return loadPropertiesFromStdin();
    } else {
      return loadPropertiesFromPath(path);
    }
  }

  private static Properties loadPropertiesFromStdin() throws InvalidArgumentException {
    Properties properties = new Properties();
    try (InputStream is = new DataInputStream(System.in)) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      // an IO error is very unlikely here
      throw new InvalidArgumentException("Failed to read config properties from stdin", e);
    }
  }

  private static Properties loadPropertiesFromPath(String path) throws InvalidArgumentException {
    Properties properties = new Properties();
    try (InputStream is = Files.newInputStream(Paths.get(path))) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new InvalidArgumentException("Failed to read config from file: '" + path + "'", e);
    }
  }

  private JmxScraper(
      JmxConnectorBuilder client, JmxMetricInsight service, JmxScraperConfig config) {
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
      try (InputStream yaml = scraperConfig.getTargetSystemYaml(system)) {
        RuleParser.get().addMetricDefsTo(config, yaml, system);
      } catch (IOException e) {
        throw new IllegalStateException("Error while loading rules for system " + system, e);
      }
    }
    for (String file : scraperConfig.getJmxConfig()) {
      addRulesFromFile(file, config);
    }
    return config;
  }

  private static void addRulesFromFile(String file, MetricConfiguration conf) {
    Path path = Paths.get(file);
    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException("Unable to read file: " + path);
    }

    try (InputStream inputStream = Files.newInputStream(path)) {
      RuleParser.get().addMetricDefsTo(conf, inputStream, file);
    } catch (IOException e) {
      throw new IllegalArgumentException("Error while loading rules from file: " + file, e);
    }
  }
}
