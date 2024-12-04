/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class implementing convenience static methods to construct data point attribute matchers
 * and sets of matchers.
 */
public class DataPointAttributes {
  private DataPointAttributes() {}

  public static AttributeMatcher attribute(String name, String value) {
    return new AttributeMatcher(name, value);
  }

  public static AttributeMatcher attributeWithAnyValue(String name) {
    return new AttributeMatcher(name);
  }

  public static Set<AttributeMatcher> attributeSet(AttributeMatcher... attributes) {
    return Arrays.stream(attributes).collect(Collectors.toSet());
  }
}
