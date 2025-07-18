/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sdk.autoconfigure;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropertyTranslatorBuilder {
  private final LinkedHashMap<String, String> translationMap = new LinkedHashMap<>();
  private final Map<String, Object> fixedValues = new LinkedHashMap<>();

  PropertyTranslatorBuilder() {}

  @CanIgnoreReturnValue
  public PropertyTranslatorBuilder addTranslation(String propertyName, String yamlPath) {
    translationMap.put(propertyName, yamlPath);
    return this;
  }

  @CanIgnoreReturnValue
  public PropertyTranslatorBuilder addFixedValue(String propertyName, Object value) {
    fixedValues.put(propertyName, value);
    return this;
  }

  PropertyTranslator build() {
    return new PropertyTranslator(translationMap, fixedValues);
  }
}
