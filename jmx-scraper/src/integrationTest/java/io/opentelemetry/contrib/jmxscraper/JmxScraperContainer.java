/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/** Test container that allows to execute {@link JmxScraper} in an isolated container */
public class JmxScraperContainer extends GenericContainer<JmxScraperContainer> {

  private final String endpoint;
  private final Set<String> targetSystems;
  private String serviceUrl;
  private final Set<String> customYamlFiles;
  private String user;
  private String password;
  private final List<String> extraJars;
  private boolean testJmx;
  private TestKeyStore keyStore;
  private TestKeyStore trustStore;
  private boolean sslRmiRegistry;

  public JmxScraperContainer(String otlpEndpoint, String baseImage) {
    super(baseImage);

    String scraperJarPath = System.getProperty("shadow.jar.path");
    assertThat(scraperJarPath).isNotNull();

    this.withCopyFileToContainer(MountableFile.forHostPath(scraperJarPath), "/scraper.jar");

    this.endpoint = otlpEndpoint;
    this.targetSystems = new HashSet<>();
    this.customYamlFiles = new HashSet<>();
    this.extraJars = new ArrayList<>();
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

  @Override
  public void start() {
    // for now only configure through JVM args
    List<String> arguments = new ArrayList<>();
    arguments.add("java");
    arguments.add("-Dotel.metrics.exporter=otlp");
    arguments.add("-Dotel.exporter.otlp.endpoint=" + endpoint);

    if (!targetSystems.isEmpty()) {
      arguments.add("-Dotel.jmx.target.system=" + String.join(",", targetSystems));
    }

    if (serviceUrl == null) {
      throw new IllegalStateException("Missing service URL");
    }
    arguments.add("-Dotel.jmx.service.url=" + serviceUrl);
    // always use a very short export interval for testing
    arguments.add("-Dotel.metric.export.interval=1s");

    if (user != null) {
      arguments.add("-Dotel.jmx.username=" + user);
    }
    if (password != null) {
      arguments.add("-Dotel.jmx.password=" + password);
    }

    arguments.addAll(addSecureStore(keyStore, /* isKeyStore= */ true));
    arguments.addAll(addSecureStore(trustStore, /* isKeyStore= */ false));

    if (sslRmiRegistry) {
      arguments.add("-Dotel.jmx.remote.registry.ssl=true");
    }

    if (!customYamlFiles.isEmpty()) {
      for (String yaml : customYamlFiles) {
        this.withCopyFileToContainer(MountableFile.forClasspathResource(yaml), yaml);
      }
      arguments.add("-Dotel.jmx.config=" + String.join(",", customYamlFiles));
    }

    if (extraJars.isEmpty()) {
      // using "java -jar" to start
      arguments.add("-jar");
      arguments.add("/scraper.jar");
    } else {
      // using "java -cp" to start
      arguments.add("-cp");
      arguments.add("/scraper.jar:" + String.join(":", extraJars));
      arguments.add("io.opentelemetry.contrib.jmxscraper.JmxScraper");
    }

    if (testJmx) {
      arguments.add("-test");
      this.waitingFor(Wait.forLogMessage(".*JMX connection test.*", 1));
    } else {
      this.waitingFor(
          Wait.forLogMessage(".*JMX scraping started.*", 1)
              .withStartupTimeout(Duration.ofSeconds(10)));
    }

    this.withCommand(arguments.toArray(new String[0]));

    logger().info("Starting scraper with command: " + String.join(" ", arguments));

    super.start();
  }

  private List<String> addSecureStore(TestKeyStore keyStore, boolean isKeyStore) {
    if (keyStore == null) {
      return Collections.emptyList();
    }
    Path path = keyStore.getPath();
    String containerPath = "/" + path.getFileName().toString();
    this.withCopyFileToContainer(MountableFile.forHostPath(path), containerPath);

    String prefix = String.format("-Djavax.net.ssl.%sStore", isKeyStore ? "key" : "trust");
    return Arrays.asList(
        prefix + "=" + containerPath, prefix + "Password=" + keyStore.getPassword());
  }
}
