/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import io.opentelemetry.contrib.jmxscraper.client.JmxRemoteClient;
import io.opentelemetry.contrib.jmxscraper.config.ConfigurationException;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfigFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class JmxScraper {
  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());

  private final JmxRemoteClient client;

  // TODO depend on instrumentation 2.9.0 snapshot
  // private final JmxMetricInsight service;

  /**
   * Main method to create and run a {@link JmxScraper} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  @SuppressWarnings({"SystemOut", "SystemExitOutsideMain"})
  public static void main(String[] args) {
    JmxScraperConfig config;
    JmxScraper jmxScraper = null;
    try {
      JmxScraperConfigFactory factory = new JmxScraperConfigFactory();
      config = JmxScraper.createConfigFromArgs(Arrays.asList(args), factory);
      jmxScraper = new JmxScraper(config);

    } catch (ArgumentsParsingException e) {
      System.err.println(
          "Usage: java -jar <path_to_jmxscraper.jar> "
              + "-config <path_to_config.properties or - for stdin>");
      System.exit(1);
    } catch (ConfigurationException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    try {
      Objects.requireNonNull(jmxScraper).start();
    } catch (IOException e) {
      System.err.println("Unable to connect " + e.getMessage());
      System.exit(2);
    }
  }

  /**
   * Create {@link JmxScraperConfig} object basing on command line options
   *
   * @param args application commandline arguments
   */
  static JmxScraperConfig createConfigFromArgs(List<String> args, JmxScraperConfigFactory factory)
      throws ArgumentsParsingException, ConfigurationException {
    if (!args.isEmpty() && (args.size() != 2 || !args.get(0).equalsIgnoreCase("-config"))) {
      throw new ArgumentsParsingException();
    }

    Properties loadedProperties = new Properties();
    if (args.size() == 2) {
      String path = args.get(1);
      if (path.trim().equals("-")) {
        loadPropertiesFromStdin(loadedProperties);
      } else {
        loadPropertiesFromPath(loadedProperties, path);
      }
    }

    return factory.createConfig(loadedProperties);
  }

  private static void loadPropertiesFromStdin(Properties props) throws ConfigurationException {
    try (InputStream is = new DataInputStream(System.in)) {
      props.load(is);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to read config properties from stdin", e);
    }
  }

  private static void loadPropertiesFromPath(Properties props, String path)
      throws ConfigurationException {
    try (InputStream is = Files.newInputStream(Paths.get(path))) {
      props.load(is);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to read config properties file: '" + path + "'", e);
    }
  }

  JmxScraper(JmxScraperConfig config) throws ConfigurationException {
    String serviceUrl = config.getServiceUrl();
    int interval = config.getIntervalMilliseconds();
    if (interval < 0) {
      throw new ConfigurationException("interval must be positive");
    }
    this.client = JmxRemoteClient.createNew(serviceUrl);
    // TODO: depend on instrumentation 2.9.0 snapshot
    // this.service = JmxMetricInsight.createService(GlobalOpenTelemetry.get(), interval);
  }

  private void start() throws IOException {

    JMXConnector connector = client.connect();

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
