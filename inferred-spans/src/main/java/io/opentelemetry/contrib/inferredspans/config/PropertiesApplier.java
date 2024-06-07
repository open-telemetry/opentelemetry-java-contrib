/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PropertiesApplier {

  private final ConfigProperties properties;

  public PropertiesApplier(ConfigProperties properties) {
    this.properties = properties;
  }

  public void applyBool(String configKey, Consumer<Boolean> funcToApply) {
    applyValue(properties.getBoolean(configKey), funcToApply);
  }

  public void applyInt(String configKey, Consumer<Integer> funcToApply) {
    applyValue(properties.getInt(configKey), funcToApply);
  }

  public void applyDuration(String configKey, Consumer<Duration> funcToApply) {
    applyValue(properties.getDuration(configKey), funcToApply);
  }

  public void applyString(String configKey, Consumer<String> funcToApply) {
    applyValue(properties.getString(configKey), funcToApply);
  }

  public void applyWildcards(
      String configKey, Consumer<? super List<WildcardMatcher>> funcToApply) {
    String wildcardListString = properties.getString(configKey);
    if (wildcardListString != null && !wildcardListString.isEmpty()) {
      List<WildcardMatcher> values =
          Arrays.stream(wildcardListString.split(","))
              .filter(str -> !str.isEmpty())
              .map(WildcardMatcher::valueOf)
              .collect(Collectors.toList());
      if (!values.isEmpty()) {
        funcToApply.accept(values);
      }
    }
  }

  private static <T> void applyValue(T value, Consumer<T> funcToApply) {
    if (value != null) {
      funcToApply.accept(value);
    }
  }
}
