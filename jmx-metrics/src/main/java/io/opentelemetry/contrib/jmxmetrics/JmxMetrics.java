/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class JmxMetrics {
  private static final Logger logger = Logger.getLogger(JmxMetrics.class.getName());

  private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
  private final GroovyRunner runner;
  private final JmxConfig config;

  JmxMetrics(final JmxConfig config) {
    this.config = config;

    JmxClient jmxClient;
    try {
      jmxClient = new JmxClient(config);
    } catch (MalformedURLException e) {
      throw new ConfigurationException("Malformed serviceUrl: ", e);
    }

    runner = new GroovyRunner(config, jmxClient, new GroovyMetricEnvironment(config));
  }

  private void start() {
    exec.scheduleWithFixedDelay(
        new Runnable() {
          @Override
          public void run() {
            try {
              runner.run();
            } catch (Throwable e) {
              logger.log(Level.SEVERE, "Error gathering JMX metrics", e);
            }
          }
        },
        0,
        config.intervalMilliseconds,
        TimeUnit.MILLISECONDS);
    logger.info("Started GroovyRunner.");
  }

  private void shutdown() {
    logger.info("Shutting down JmxMetrics Groovy runner and exporting final metrics.");
    exec.shutdown();
    runner.shutdown();
  }

  private static JmxConfig getConfigFromArgs(final String[] args) {
    if (args.length != 0 && (args.length != 2 || !args[0].equalsIgnoreCase("-config"))) {
      System.out.println(
          "Usage: java io.opentelemetry.contrib.jmxmetrics.JmxMetrics "
              + "-config <path_to_config.properties or - for stdin>");
      System.exit(1);
    }

    Properties props = new Properties();
    if (args.length == 2) {
      String path = args[1];
      if (path.trim().equals("-")) {
        loadPropertiesFromStdin(props);
      } else {
        loadPropertiesFromPath(props, path);
      }
    }

    return new JmxConfig(props);
  }

  private static void loadPropertiesFromStdin(Properties props) {
    try (InputStream is = new DataInputStream(System.in)) {
      props.load(is);
    } catch (IOException e) {
      System.out.println("Failed to read config properties from stdin: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void loadPropertiesFromPath(Properties props, String path) {
    try (InputStream is = new FileInputStream(path)) {
      props.load(is);
    } catch (IOException e) {
      System.out.println(
          "Failed to read config properties file at '" + path + "': " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Main method to create and run a {@link JmxMetrics} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  public static void main(final String[] args) {
    JmxConfig config = getConfigFromArgs(args);
    config.validate();

    final JmxMetrics jmxMetrics = new JmxMetrics(config);
    jmxMetrics.start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                jmxMetrics.shutdown();
              }
            });
  }
}
