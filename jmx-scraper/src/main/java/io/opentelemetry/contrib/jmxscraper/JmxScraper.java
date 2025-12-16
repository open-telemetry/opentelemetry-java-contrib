/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_INSTANCE_ID;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.contrib.jmxscraper.config.PropertiesCustomizer;
import io.opentelemetry.contrib.jmxscraper.config.PropertiesSupplier;
import io.opentelemetry.instrumentation.jmx.JmxTelemetry;
import io.opentelemetry.instrumentation.jmx.JmxTelemetryBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.resources.Resource;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public final class JmxScraper {

  private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());
  private static final String CONFIG_ARG = "-config";
  private static final String TEST_ARG = "-test";

  private final JmxConnectorBuilder client;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final JmxTelemetry jmxTelemetry;

  /**
   * Main method to create and run a {@link JmxScraper} instance.
   *
   * @param args - must be of the form "-config {jmx_config_path,'-'}"
   */
  @SuppressWarnings("SystemExitOutsideMain")
  public static void main(String[] args) {

    // set log format
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%n");

    List<String> effectiveArgs = new ArrayList<>(asList(args));
    boolean testMode = effectiveArgs.remove(TEST_ARG);

    try {
      Properties argsConfig = argsToConfig(effectiveArgs);
      propagateToSystemProperties(argsConfig);

      PropertiesCustomizer configCustomizer = new PropertiesCustomizer();

      // we rely on the config customizer to be executed first to get effective config.
      BiFunction<Resource, ConfigProperties, Resource> resourceCustomizer =
          (resource, configProperties) -> {
            UUID instanceId = getRemoteServiceInstanceId(configCustomizer.getConnectorBuilder());
            if (resource.getAttribute(SERVICE_INSTANCE_ID) != null || instanceId == null) {
              return resource;
            }
            logger.log(INFO, "remote service instance ID: " + instanceId);
            return resource.merge(
                Resource.create(Attributes.of(SERVICE_INSTANCE_ID, instanceId.toString())));
          };

      // auto-configure SDK
      OpenTelemetry openTelemetry =
          AutoConfiguredOpenTelemetrySdk.builder()
              .addPropertiesSupplier(new PropertiesSupplier(argsConfig))
              .addPropertiesCustomizer(configCustomizer)
              .addResourceCustomizer(resourceCustomizer)
              .build()
              .getOpenTelemetrySdk();

      // scraper configuration and connector builder are built using effective SDK configuration
      // thus we have to get it after the SDK is built
      JmxScraperConfig scraperConfig = configCustomizer.getScraperConfig();
      JmxConnectorBuilder connectorBuilder = configCustomizer.getConnectorBuilder();

      if (testMode) {
        System.exit(testConnection(connectorBuilder) ? 0 : 1);
      } else {
        JmxScraper jmxScraper = new JmxScraper(connectorBuilder, openTelemetry, scraperConfig);
        jmxScraper.start();
      }
    } catch (ConfigurationException e) {
      logger.log(SEVERE, "invalid configuration: " + e.getMessage(), e);
      System.exit(1);
    } catch (InvalidArgumentException e) {
      logger.log(SEVERE, e.getMessage(), e);
      logger.info("Usage: java -jar <path_to_jmxscraper.jar> [-test] [-config <conf>]");
      logger.info("  -test           test JMX connection with provided configuration and exit");
      logger.info(
          "  -config <conf>  provide configuration, where <conf> is - for stdin, or <path_to_config.properties>");
      System.exit(1);
    } catch (IOException e) {
      logger.log(SEVERE, "Unable to connect ", e);
      System.exit(2);
    } catch (RuntimeException e) {
      logger.log(SEVERE, e.getMessage(), e);
      System.exit(3);
    }
  }

  private static boolean testConnection(JmxConnectorBuilder connectorBuilder) {
    try (JMXConnector connector = connectorBuilder.build()) {
      MBeanServerConnection connection = connector.getMBeanServerConnection();
      Integer mbeanCount = connection.getMBeanCount();
      if (mbeanCount > 0) {
        logger.log(INFO, "JMX connection test OK");
        return true;
      } else {
        logger.log(SEVERE, "JMX connection test ERROR");
        return false;
      }
    } catch (IOException e) {
      logger.log(SEVERE, "JMX connection test ERROR", e);
      return false;
    }
  }

  @Nullable
  private static UUID getRemoteServiceInstanceId(JmxConnectorBuilder connectorBuilder) {
    try (JMXConnector jmxConnector = connectorBuilder.build()) {
      MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

      StringBuilder id = new StringBuilder();
      try {
        ObjectName objectName = new ObjectName("java.lang:type=Runtime");
        for (String attribute : Arrays.asList("StartTime", "Name")) {
          Object value = connection.getAttribute(objectName, attribute);
          if (id.length() > 0) {
            id.append(" ");
          }
          id.append(value);
        }
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    } catch (IOException e) {
      return null;
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
      JmxConnectorBuilder client, OpenTelemetry openTelemetry, JmxScraperConfig config) {
    this.client = client;
    this.jmxTelemetry = createJmxTelemetry(openTelemetry, config);
  }

  private static JmxTelemetry createJmxTelemetry(
      OpenTelemetry openTelemetry, JmxScraperConfig config) {

    JmxTelemetryBuilder builder = JmxTelemetry.builder(openTelemetry);
    builder.beanDiscoveryDelay(config.getSamplingInterval());

    config
        .getTargetSystems()
        .forEach(
            system -> {
              try (InputStream input = config.getTargetSystemYaml(system)) {
                builder.addRules(input);
              }
            });

    config.getJmxConfig().stream().map(Paths::get).forEach(path -> builder.addRules(path));
    return builder.build();
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

      jmxTelemetry.start(() -> singletonList(connection));

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
}
