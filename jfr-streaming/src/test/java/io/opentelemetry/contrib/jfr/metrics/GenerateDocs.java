/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static java.util.stream.Collectors.joining;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenerateDocs {

  private static final Logger LOGGER = Logger.getLogger(GenerateDocs.class.getName());

  private static final String JFR_README_PATH_KEY = "jfr.readme.path";
  private static final String START = "<!-- generateDocsStart -->";
  private static final String END = "<!-- generateDocsEnd -->";
  private static final Pattern PATTERN = Pattern.compile(START + ".*" + END, Pattern.DOTALL);

  private GenerateDocs() {}

  public static void main(String[] args) throws Exception {
    // Suppress info level logs
    Logger.getLogger(JfrTelemetry.class.getName()).setLevel(Level.WARNING);

    String jfrReadmePath = System.getProperty(JFR_README_PATH_KEY);
    if (jfrReadmePath == null) {
      throw new IllegalStateException(JFR_README_PATH_KEY + " is required");
    }

    LOGGER.info("Generating JFR docs. Writing to " + jfrReadmePath);
    String markdownTable = generateMarkdownTable();
    LOGGER.info("Markdown table: " + System.lineSeparator() + markdownTable);
    writeReadme(markdownTable, jfrReadmePath);
    LOGGER.info("Done");
  }

  private static String generateMarkdownTable() throws InterruptedException {
    Map<JfrFeature, String> jfrFeatureRows = new ConcurrentHashMap<>();
    List<Thread> threads = new ArrayList<>();
    for (JfrFeature feature : JfrFeature.values()) {
      Thread thread =
          new Thread(
              () -> {
                try {
                  String metricCol =
                      metricsForJfrFeature(feature).stream()
                          .sorted()
                          .map(s -> "`" + s + "`")
                          .collect(joining(", "));
                  jfrFeatureRows.put(
                      feature,
                      "| "
                          + feature.name()
                          + " | "
                          + feature.isDefaultEnabled()
                          + " | "
                          + metricCol
                          + " |"
                          + System.lineSeparator());
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });
      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    StringBuilder table =
        new StringBuilder("| JfrFeature | Default Enabled | Metrics |")
            .append(System.lineSeparator())
            .append("|---|---|---|")
            .append(System.lineSeparator());
    for (JfrFeature feature : JfrFeature.values()) {
      table.append(jfrFeatureRows.get(feature));
    }

    return table.toString();
  }

  private static Set<String> metricsForJfrFeature(JfrFeature jfrFeature)
      throws InterruptedException {
    InMemoryMetricReader reader = InMemoryMetricReader.createDelta();
    OpenTelemetrySdk sdk =
        OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
    try (JfrTelemetry unused =
        JfrTelemetry.builder(sdk).disableAllFeatures().enableFeature(jfrFeature).build()) {
      System.gc();
      executeDummyNetworkRequest("https://google.com");
      Thread.sleep(2000);
      return reader.collectAllMetrics().stream()
          .map(MetricData::getName)
          .collect(Collectors.toSet());
    } finally {
      sdk.getSdkMeterProvider().close();
    }
  }

  private static void executeDummyNetworkRequest(String urlString) {
    try {
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.getResponseCode();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to execute request", e);
    }
  }

  private static void writeReadme(String markdownTable, String jfrReadmePath) throws IOException {
    Path path = Paths.get(jfrReadmePath);
    String readmeContent = Files.readString(path);
    readmeContent =
        PATTERN
            .matcher(readmeContent)
            .replaceAll(
                START
                    + System.lineSeparator()
                    + System.lineSeparator()
                    + markdownTable
                    + System.lineSeparator()
                    + END);
    Files.writeString(path, readmeContent);
  }
}
