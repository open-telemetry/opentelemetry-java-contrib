/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import io.opentelemetry.contrib.jmxscraper.config.ConfigurationException;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class JmxScraper {
  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());
  private static final String CONFIG_ARG = "-config";

  private final JmxConnectorBuilder client;

  // TODO depend on instrumentation 2.9.0 snapshot
  // private final JmxMetricInsight service;

  /**
   * Main method to create and run a {@link JmxScraper} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  @SuppressWarnings({"SystemOut", "SystemExitOutsideMain"})
  public static void main(String[] args) {
    try {
      JmxScraperConfig config =
          JmxScraperConfig.fromProperties(parseArgs(Arrays.asList(args)), System.getProperties());
      // propagate effective user-provided configuration to JVM system properties
      config.propagateSystemProperties();
      // TODO: depend on instrumentation 2.9.0 snapshot
      // service = JmxMetricInsight.createService(GlobalOpenTelemetry.get(),
      // config.getIntervalMilliseconds());
      JmxScraper jmxScraper = new JmxScraper(JmxConnectorBuilder.createNew(config.getServiceUrl()));
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

  JmxScraper(JmxConnectorBuilder client) {
    this.client = client;
  }

  private void start() throws IOException {

    JMXConnector connector = client.build();

    @SuppressWarnings("unused")
    MBeanServerConnection connection = connector.getMBeanServerConnection();

    // TODO: depend on instrumentation 2.9.0 snapshot
    // MetricConfiguration metricConfig = new MetricConfiguration();
    // TODO create JMX insight config from scraper config
    // service.startRemote(metricConfig, () -> Collections.singletonList(connection));

    logger.info("JMX scraping started");

    // TODO: wait a bit to keep the JVM running, this won't be needed once calling jmx insight
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}
