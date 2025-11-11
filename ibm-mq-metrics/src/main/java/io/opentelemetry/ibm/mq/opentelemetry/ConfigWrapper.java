/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/** Low-fi domain-specific yaml wrapper. */
public final class ConfigWrapper {

  private static final Logger logger = LoggerFactory.getLogger(ConfigWrapper.class);

  private static final int DEFAULT_THREADS = 20;
  private static final int DEFAULT_DELAY_SECONDS = 60;
  private static final int DEFAULT_INITIAL_DELAY = 0;

  private final Map<String, ?> config;

  private ConfigWrapper(Map<String, ?> config) {
    this.config = config;
  }

  public static ConfigWrapper parse(String configFile) throws IOException {
    Yaml yaml = new Yaml();
    Map<String, ?> config =
        yaml.load(Files.newBufferedReader(Paths.get(configFile), Charset.defaultCharset()));
    return new ConfigWrapper(config);
  }

  public int getNumberOfThreads() {
    int value = defaultedInt(getTaskSchedule(), "numberOfThreads", DEFAULT_THREADS);
    if (value < DEFAULT_THREADS) {
      logger.warn(
          "numberOfThreads {} is less than the minimum number of threads allowed. Using {} instead.",
          value,
          DEFAULT_THREADS);
      value = DEFAULT_THREADS;
    }
    return value;
  }

  int getTaskDelaySeconds() {
    return defaultedInt(getTaskSchedule(), "taskDelaySeconds", DEFAULT_DELAY_SECONDS);
  }

  Duration getTaskDelay() {
    return Duration.ofSeconds(getTaskDelaySeconds());
  }

  int getTaskInitialDelaySeconds() {
    return defaultedInt(getTaskSchedule(), "initialDelaySeconds", DEFAULT_INITIAL_DELAY);
  }

  @NotNull
  List<String> getQueueManagerNames() {
    return getQueueManagers().stream()
        .map(x -> String.valueOf(x.get("name")))
        .collect(Collectors.toList());
  }

  @NotNull
  @SuppressWarnings("unchecked") // reading list from yaml
  public List<Map<String, ?>> getQueueManagers() {
    List<Map<String, ?>> result = (List<Map<String, ?>>) config.get("queueManagers");
    if (result == null) {
      return emptyList();
    }
    return result;
  }

  @NotNull
  @SuppressWarnings("unchecked") // reading map from yaml
  public Map<String, String> getSslConnection() {
    Map<String, String> result = (Map<String, String>) config.get("sslConnection");
    if (result == null) {
      return Collections.emptyMap();
    }
    return result;
  }

  public int getInt(String key, int defaultValue) {
    Object result = config.get(key);
    if (result == null) {
      return defaultValue;
    }
    return (Integer) result;
  }

  @NotNull
  @SuppressWarnings("unchecked") // reading map from yaml
  public Map<String, ?> getMetrics() {
    Object metrics = config.get("metrics");
    if (!(metrics instanceof Map)) {
      throw new IllegalArgumentException("config metrics section is missing");
    }
    return (Map<String, ?>) metrics;
  }

  private static int defaultedInt(Map<String, ?> section, String key, int defaultValue) {
    Object val = section.get(key);
    return val instanceof Integer ? (Integer) val : defaultValue;
  }

  @SuppressWarnings("unchecked") // reading map from yaml
  private Map<String, ?> getTaskSchedule() {
    if (config.get("taskSchedule") instanceof Map) {
      return (Map<String, ?>) config.get("taskSchedule");
    }
    return Collections.emptyMap();
  }
}
