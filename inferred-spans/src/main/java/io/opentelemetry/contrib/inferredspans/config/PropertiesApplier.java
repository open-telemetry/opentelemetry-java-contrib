/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
