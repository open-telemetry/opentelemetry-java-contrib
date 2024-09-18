/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
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
  private int intervalMillis;
  private final Set<String> customYaml;

  public JmxScraperContainer(String otlpEndpoint) {
    super("openjdk:8u272-jre-slim");

    String scraperJarPath = System.getProperty("shadow.jar.path");
    assertThat(scraperJarPath).isNotNull();

    this.withCopyFileToContainer(MountableFile.forHostPath(scraperJarPath), "/scraper.jar")
        .waitingFor(
            Wait.forLogMessage(".*JMX scraping started.*", 1)
                .withStartupTimeout(Duration.ofSeconds(10)));

    this.endpoint = otlpEndpoint;
    this.targetSystems = new HashSet<>();
    this.customYaml = new HashSet<>();
    this.intervalMillis = 1000;
  }

  public JmxScraperContainer withTargetSystem(String targetSystem) {
    targetSystems.add(targetSystem);
    return this;
  }

  public JmxScraperContainer withIntervalMillis(int intervalMillis) {
    this.intervalMillis = intervalMillis;
    return this;
  }

  public JmxScraperContainer withService(String host, int port) {
    // TODO: adding a way to provide 'host:port' syntax would make this easier for end users
    this.serviceUrl =
        String.format(
            Locale.getDefault(), "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port);
    return this;
  }

  public JmxScraperContainer withCustomYaml(String yamlPath) {
    this.customYaml.add(yamlPath);
    return this;
  }

  @Override
  public void start() {
    // for now only configure through JVM args
    List<String> arguments = new ArrayList<>();
    arguments.add("java");
    arguments.add("-Dotel.exporter.otlp.endpoint=" + endpoint);

    if (!targetSystems.isEmpty()) {
      arguments.add("-Dotel.jmx.target.system=" + String.join(",", targetSystems));
    }

    if (serviceUrl == null) {
      throw new IllegalStateException("Missing service URL");
    }
    arguments.add("-Dotel.jmx.service.url=" + serviceUrl);
    arguments.add("-Dotel.jmx.interval.milliseconds=" + intervalMillis);

    if (!customYaml.isEmpty()) {
      int i = 0;
      StringBuilder sb = new StringBuilder("-Dotel.jmx.config=");
      for (String yaml : customYaml) {
        String containerPath = "/custom_" + i + ".yaml";
        this.withCopyFileToContainer(MountableFile.forClasspathResource(yaml), containerPath);
        if (i > 0) {
          sb.append(",");
        }
        sb.append(containerPath);
        i++;
      }
      arguments.add(sb.toString());
    }

    arguments.add("-jar");
    arguments.add("/scraper.jar");

    this.withCommand(arguments.toArray(new String[0]));

    super.start();
  }
}
