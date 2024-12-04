/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import java.util.Objects;

class AttributeValueMatcher {
  static final AttributeValueMatcher ANY_VALUE_MATCHER = new AttributeValueMatcher();

  private final String value;

  AttributeValueMatcher() {
    this(null);
  }

  AttributeValueMatcher(String value) {
    this.value = value;
  }

  String getValue() {
    return value;
  }

  /**
   * Match the value held by this and the other AttributeValueMatcher instances. Null value means
   * "any value", that's why if any of the values is null they are considered as matching. If both
   * values are not null then they must be equal.
   *
   * @param other the other matcher to compare value with
   * @return true if values held by both matchers are matching, false otherwise.
   */
  boolean matchValue(AttributeValueMatcher other) {
    if ((value == null) || (other.value == null)) {
      return true;
    }
    return Objects.equals(value, other.value);
  }
}
