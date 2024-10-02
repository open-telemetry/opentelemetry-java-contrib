/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.contrib.jmxscraper.config.ConfigurationException;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.instrumentation.jmx.engine.JmxMetricInsight;
import io.opentelemetry.instrumentation.jmx.engine.MetricConfiguration;
import io.opentelemetry.instrumentation.jmx.yaml.RuleParser;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class JmxScraper {
  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());
  private static final String CONFIG_ARG = "-config";

  private static final String OTEL_AUTOCONFIGURE = "otel.java.global-autoconfigure.enabled";

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

    // enable SDK auto-configure if not explicitly set by user
    // TODO: refactor this to use AutoConfiguredOpenTelemetrySdk
    if (System.getProperty(OTEL_AUTOCONFIGURE) == null) {
      System.setProperty(OTEL_AUTOCONFIGURE, "true");
    }

    try {
      JmxScraperConfig config =
          JmxScraperConfig.fromProperties(parseArgs(Arrays.asList(args)), System.getProperties());
      // propagate effective user-provided configuration to JVM system properties
      // this also enables SDK auto-configuration to use those properties
      config.propagateSystemProperties();

      JmxMetricInsight service =
          JmxMetricInsight.createService(
              GlobalOpenTelemetry.get(), config.getIntervalMilliseconds());
      JmxScraper jmxScraper =
          new JmxScraper(JmxConnectorBuilder.createNew(config.getServiceUrl()), service, config);
      jmxScraper.start();

    } catch (ArgumentsParsingException e) {
      System.err.println("ERROR: " + e.getMessage());
      System.err.println(
          "Usage: java -jar <path_to_jmxscraper.jar> "
              + "-config <path_to_config.properties or - for stdin>");
      System.exit(1);
    } catch (ConfigurationException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Unable to connect " + e.getMessage());
      System.exit(2);
    }
  }

  /**
   * Create {@link Properties} from command line options
   *
   * @param args application commandline arguments
   */
  static Properties parseArgs(List<String> args)
      throws ArgumentsParsingException, ConfigurationException {

    if (args.isEmpty()) {
      // empty properties from stdin or external file
      // config could still be provided through JVM system properties
      return new Properties();
    }
    if (args.size() != 2) {
      throw new ArgumentsParsingException("exactly two arguments expected, got " + args.size());
    }
    if (!args.get(0).equalsIgnoreCase(CONFIG_ARG)) {
      throw new ArgumentsParsingException("unexpected first argument must be '" + CONFIG_ARG + "'");
    }

    String path = args.get(1);
    if (path.trim().equals("-")) {
      return loadPropertiesFromStdin();
    } else {
      return loadPropertiesFromPath(path);
    }
  }

  private static Properties loadPropertiesFromStdin() throws ConfigurationException {
    Properties properties = new Properties();
    try (InputStream is = new DataInputStream(System.in)) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new ConfigurationException("Failed to read config properties from stdin", e);
    }
  }

  private static Properties loadPropertiesFromPath(String path) throws ConfigurationException {
    Properties properties = new Properties();
    try (InputStream is = Files.newInputStream(Paths.get(path))) {
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new ConfigurationException("Failed to read config properties file: '" + path + "'", e);
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
      try {
        addRulesForSystem(system, config);
      } catch (RuntimeException e) {
        logger.warning("unable to load rules for system " + system + ": " + e.getMessage());
      }
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
        throw new IllegalStateException("no support for " + system);
      }
    } catch (Exception e) {
      throw new IllegalStateException("error while loading rules for system " + system, e);
    }
  }
}
