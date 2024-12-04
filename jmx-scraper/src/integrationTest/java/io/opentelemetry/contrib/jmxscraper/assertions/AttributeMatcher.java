/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import static io.opentelemetry.contrib.jmxscraper.assertions.AttributeValueMatcher.ANY_VALUE_MATCHER;

import java.util.Objects;

/** Implements functionality of matching data point attributes. */
public class AttributeMatcher {
  private final String name;
  private final AttributeValueMatcher attributeValueMatcher;

  /**
   * Create instance used to match data point attribute with te same name and with any value.
   *
   * @param name attribute name
   */
  AttributeMatcher(String name) {
    this.name = name;
    this.attributeValueMatcher = ANY_VALUE_MATCHER;
  }

  /**
   * Create instance used to match data point attribute with te same name and with the same value.
   *
   * @param name attribute name
   * @param value attribute value
   */
  AttributeMatcher(String name, String value) {
    this.name = name;
    this.attributeValueMatcher = new AttributeValueMatcher(value);
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttributeMatcher)) {
      return false;
    }
    AttributeMatcher other = (AttributeMatcher) o;
    return Objects.equals(name, other.name)
        && attributeValueMatcher.matchValue(other.attributeValueMatcher);
  }

  @Override
  public int hashCode() {
    // Do not use value matcher here to support value wildcards
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return attributeValueMatcher.getValue() == null
        ? '{' + name + '}'
        : '{' + name + '=' + attributeValueMatcher.getValue() + '}';
  }
  ;
}
