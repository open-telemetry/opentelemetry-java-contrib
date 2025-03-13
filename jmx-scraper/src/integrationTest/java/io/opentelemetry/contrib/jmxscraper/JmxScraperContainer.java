/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/** Test container that allows to execute {@link JmxScraper} in an isolated container */
public class JmxScraperContainer extends GenericContainer<JmxScraperContainer> {

  private final String endpoint;
  private final Set<String> targetSystems;
  @Nullable private String targetSystemSource;
  private String serviceUrl;
  private final Set<String> customYamlFiles;
  private String user;
  private String password;
  private final List<String> extraJars;
  private boolean testJmx;
  private TestKeyStore keyStore;
  private TestKeyStore trustStore;
  private boolean sslRmiRegistry;
  private ConfigSource configSource;

  /** Defines different strategies to provide scraper configuration */
  public enum ConfigSource {
    /** system properties with "-D" prefix in JVM command */
    SYSTEM_PROPERTIES,
    /** properties file */
    PROPERTIES_FILE,
    /** standard input */
    STDIN,
    /** environment variables with "OTEL_" prefix, non-otel options as system properties */
    ENVIRONMENT_VARIABLES;
  }

  public JmxScraperContainer(String otlpEndpoint, String baseImage) {
    super(baseImage);

    String scraperJarPath = System.getProperty("shadow.jar.path");
    assertThat(scraperJarPath).isNotNull();

    this.withCopyFileToContainer(MountableFile.forHostPath(scraperJarPath), "/scraper.jar");

    this.endpoint = otlpEndpoint;
    this.targetSystems = new HashSet<>();
    this.customYamlFiles = new HashSet<>();
    this.extraJars = new ArrayList<>();
    this.configSource = ConfigSource.SYSTEM_PROPERTIES;
  }

  /**
   * Adds a target system
   *
   * @param targetSystem target system
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withTargetSystem(String targetSystem) {
    targetSystems.add(targetSystem);
    return this;
  }

  /**
   * Sets the target system source
   *
   * @param source target system source, valid values are "auto", "instrumentation" and "legacy"
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withTargetSystemSource(String source) {
    this.targetSystemSource = source;
    return this;
  }

  /**
   * Set connection to a standard JMX service URL
   *
   * @param host JMX host
   * @param port JMX port
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withRmiServiceUrl(String host, int port) {
    return withServiceUrl(
        String.format(
            Locale.getDefault(), "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port));
  }

  /**
   * Set connection to a JMX service URL
   *
   * @param serviceUrl service URL
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
    return this;
  }

  /**
   * Sets JMX user login
   *
   * @param user user login
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * Sets JMX password
   *
   * @param password user password
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Adds path to an extra jar for classpath
   *
   * @param jarPath path to an extra jar that should be added to jmx scraper classpath
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withExtraJar(String jarPath) {
    this.extraJars.add(jarPath);
    return this;
  }

  /**
   * Adds custom metrics yaml from classpath resource
   *
   * @param yamlPath path to resource in classpath
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withCustomYaml(String yamlPath) {
    this.customYamlFiles.add(yamlPath);
    return this;
  }

  /**
   * Configure the scraper JVM to only test connection with the JMX endpoint
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withTestJmx() {
    this.testJmx = true;
    return this;
  }

  /**
   * Configure key store for the scraper JVM
   *
   * @param keyStore key store
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withKeyStore(TestKeyStore keyStore) {
    this.keyStore = keyStore;
    return this;
  }

  /**
   * Configure trust store for the scraper JVM
   *
   * @param trustStore trust store
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withTrustStore(TestKeyStore trustStore) {
    this.trustStore = trustStore;
    return this;
  }

  /**
   * Enables connection to an SSL-protected RMI registry
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withSslRmiRegistry() {
    this.sslRmiRegistry = true;
    return this;
  }

  /**
   * Sets how configuration is provided to scraper
   *
   * @param source configuration source
   * @return this
   */
  @CanIgnoreReturnValue
  public JmxScraperContainer withConfigSource(ConfigSource source) {
    this.configSource = source;
    return this;
  }

  @Override
  public void start() {

    Map<String, String> config = initConfig();

    List<String> cmd = createCommand(config);

    if (configSource != ConfigSource.STDIN) {
      this.withCommand(cmd.toArray(new String[0]));
    } else {
      Path script = generateShellScript(cmd, config);

      this.withCopyFileToContainer(MountableFile.forHostPath(script, 500), "/scraper.sh");
      this.withCommand("/scraper.sh");
    }

    logger().info("Starting scraper with command: " + String.join(" ", this.getCommandParts()));
    super.start();
  }

  private Path generateShellScript(List<String> cmd, Map<String, String> config) {
    // generate shell script to feed standard input with config
    List<String> lines = new ArrayList<>();
    lines.add("#!/bin/bash");
    lines.add(String.join(" ", cmd) + "<<EOF");
    lines.addAll(toKeyValueString(config));
    lines.add("EOF");

    Path script;
    try {
      script = Files.createTempFile("scraper", ".sh");
      Files.write(script, lines);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    logger().info("Scraper executed with /scraper.sh shell script");
    for (int i = 0; i < lines.size(); i++) {
      logger().info("/scrapper.sh:{}  {}", i, lines.get(i));
    }
    return script;
  }

  private List<String> createCommand(Map<String, String> config) {
    List<String> cmd = new ArrayList<>();
    cmd.add("java");

    switch (configSource) {
      case SYSTEM_PROPERTIES:
        cmd.addAll(
            toKeyValueString(config).stream().map(s -> "-D" + s).collect(Collectors.toList()));
        break;
      case PROPERTIES_FILE:
        try {
          Path configFile = Files.createTempFile("config", ".properties");
          Files.write(configFile, toKeyValueString(config));
          this.withCopyFileToContainer(MountableFile.forHostPath(configFile), "/config.properties");
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        break;
      case STDIN:
        // nothing needed here
        break;
      case ENVIRONMENT_VARIABLES:
        Map<String, String> env = new HashMap<>();
        Map<String, String> other = new HashMap<>();
        config.forEach(
            (k, v) -> {
              if (k.startsWith("otel.")) {
                env.put(k.toUpperCase(Locale.ROOT).replace(".", "_"), v);
              } else {
                other.put(k, v);
              }
            });

        if (!other.isEmpty()) {
          env.put(
              "JAVA_TOOL_OPTIONS",
              toKeyValueString(other).stream().map(s -> "-D" + s).collect(Collectors.joining(" ")));
        }
        this.withEnv(env);
        env.forEach((k, v) -> logger().info("Using environment variable {} = {} ", k, v));

        break;
    }

    if (extraJars.isEmpty()) {
      // using "java -jar" to start
      cmd.add("-jar");
      cmd.add("/scraper.jar");
    } else {
      // using "java -cp" to start
      cmd.add("-cp");
      cmd.add("/scraper.jar:" + String.join(":", extraJars));
      cmd.add("io.opentelemetry.contrib.jmxscraper.JmxScraper");
    }

    switch (configSource) {
      case SYSTEM_PROPERTIES:
      case ENVIRONMENT_VARIABLES:
        // no extra program argument needed
        break;
      case PROPERTIES_FILE:
        cmd.add("-config");
        cmd.add("/config.properties");
        break;
      case STDIN:
        cmd.add("-config");
        cmd.add("-");
        break;
    }

    if (testJmx) {
      cmd.add("-test");
      this.waitingFor(Wait.forLogMessage(".*JMX connection test.*", 1));
    } else {
      this.waitingFor(
          Wait.forLogMessage(".*JMX scraping started.*", 1)
              .withStartupTimeout(Duration.ofSeconds(10)));
    }
    return cmd;
  }

  private Map<String, String> initConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("otel.metrics.exporter", "otlp");
    config.put("otel.exporter.otlp.endpoint", endpoint);

    if (!targetSystems.isEmpty()) {
      config.put("otel.jmx.target.system", String.join(",", targetSystems));

      // rely on default when explicitly set
      if (targetSystemSource != null) {
        config.put("otel.jmx.target.source", targetSystemSource);
      }
    }

    if (serviceUrl == null) {
      throw new IllegalStateException("Missing service URL");
    }
    config.put("otel.jmx.service.url", serviceUrl);

    // always use a very short export interval for testing
    config.put("otel.metric.export.interval", "1s");

    if (user != null) {
      config.put("otel.jmx.username", user);
    }
    if (password != null) {
      config.put("otel.jmx.password", password);
    }

    addSecureStore(keyStore, /* isKeyStore= */ true, config);
    addSecureStore(trustStore, /* isKeyStore= */ false, config);

    if (sslRmiRegistry) {
      config.put("otel.jmx.remote.registry.ssl", "true");
    }

    if (!customYamlFiles.isEmpty()) {
      for (String yaml : customYamlFiles) {
        this.withCopyFileToContainer(MountableFile.forClasspathResource(yaml), yaml);
      }
      config.put("otel.jmx.config", String.join(",", customYamlFiles));
    }
    return config;
  }

  private void addSecureStore(
      TestKeyStore keyStore, boolean isKeyStore, Map<String, String> config) {
    if (keyStore == null) {
      return;
    }
    Path path = keyStore.getPath();
    String containerPath = "/" + path.getFileName().toString();
    this.withCopyFileToContainer(MountableFile.forHostPath(path), containerPath);

    String prefix = String.format("javax.net.ssl.%sStore", isKeyStore ? "key" : "trust");

    config.put(prefix, containerPath);
    config.put(prefix + "Password", keyStore.getPassword());
  }

  private static List<String> toKeyValueString(Map<String, String> options) {
    return options.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }
}
