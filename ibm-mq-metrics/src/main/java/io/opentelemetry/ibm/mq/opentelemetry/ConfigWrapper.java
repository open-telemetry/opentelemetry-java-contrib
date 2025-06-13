/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.emptyList;

import java.io.FileNotFoundException;
import java.io.FileReader;
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

  public static ConfigWrapper parse(String configFile) throws FileNotFoundException {
    Yaml yaml = new Yaml();
    Map<String, ?> config = yaml.load(new FileReader(configFile));
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

  private int defaultedInt(Map<String, ?> section, String key, int defaultValue) {
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
