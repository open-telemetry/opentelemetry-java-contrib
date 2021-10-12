/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

final class OtelUtils {

  public static String getComaSeparatedString(Map<String, String> keyValuePairs) {
    return keyValuePairs.entrySet().stream()
        .map(keyValuePair -> keyValuePair.getKey() + "=" + keyValuePair.getValue())
        .collect(Collectors.joining(","));
  }

  public static Map<String, String> getCommaSeparatedMap(String comaSeparatedKeyValuePairs) {
    if (StringUtils.isBlank(comaSeparatedKeyValuePairs)) {
      return new HashMap<>();
    }
    return filterBlanksAndNulls(comaSeparatedKeyValuePairs.split(",")).stream()
        .map(keyValuePair -> filterBlanksAndNulls(keyValuePair.split("=", 2)))
        .map(
            splitKeyValuePairs -> {
              if (splitKeyValuePairs.size() != 2) {
                throw new RuntimeException("Invalid key-value pair: " + comaSeparatedKeyValuePairs);
              }
              return new AbstractMap.SimpleImmutableEntry<>(
                  splitKeyValuePairs.get(0), splitKeyValuePairs.get(1));
            })
        // If duplicate keys, prioritize later ones similar to duplicate system properties on a
        // Java command line.
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (first, next) -> next, LinkedHashMap::new));
  }

  private static List<String> filterBlanksAndNulls(String[] values) {
    return Arrays.stream(values)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  @CheckForNull
  public static String getSystemPropertyOrEnvironmentVariable(
      String systemPropertyName, String environmentVariableName, @Nullable String defaultValue) {
    String systemProperty = System.getProperty(systemPropertyName);
    if (StringUtils.isNotBlank(systemProperty)) {
      return systemProperty;
    }
    String environmentVariable = System.getenv(environmentVariableName);
    if (StringUtils.isNotBlank(environmentVariable)) {
      return environmentVariable;
    }
    return defaultValue;
  }
}
