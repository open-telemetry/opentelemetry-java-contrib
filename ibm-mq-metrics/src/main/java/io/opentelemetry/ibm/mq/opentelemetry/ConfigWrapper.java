/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    return parse(new URL("file://" + configFile));
  }

  public static ConfigWrapper parse(URL configFile) throws IOException {
    Yaml yaml = new Yaml();
    try (InputStream in = configFile.openStream()) {
      Map<String, ?> config = yaml.load(in);
      return new ConfigWrapper(config);
    }
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
  @SuppressWarnings("unchecked")
  List<String> getQueueManagerNames() {
    return getQueueManagers().stream()
        .map(o -> (Map<String, String>) o)
        .map(x -> x.get("name"))
        .collect(Collectors.toList());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public List<Map<String, ?>> getQueueManagers() {
    List<Map<String, ?>> result = (List<Map<String, ?>>) config.get("queueManagers");
    if (result == null) {
      return emptyList();
    }
    return result;
  }

  @NotNull
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  private Map<String, ?> getTaskSchedule() {
    if (config.get("taskSchedule") instanceof Map) {
      return (Map<String, ?>) config.get("taskSchedule");
    }
    return Collections.emptyMap();
  }
}
