/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import java.util.Objects;

/** Implements functionality of matching data point attributes. */
public class AttributeMatcher {
  private final String attributeName;
  private final String attributeValue;

  /**
   * Create instance used to match data point attribute with any value.
   *
   * @param attributeName matched attribute name
   */
  AttributeMatcher(String attributeName) {
    this.attributeName = attributeName;
    this.attributeValue = null;
  }

  /**
   * Create instance used to match data point attribute with te same name and with the same value.
   *
   * @param attributeName attribute name
   * @param attributeValue attribute value
   */
  AttributeMatcher(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  /**
   * Return name of data point attribute that this AttributeMatcher is supposed to match value with.
   *
   * @return name of validated attribute
   */
  public String getAttributeName() {
    return attributeName;
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
    return Objects.equals(attributeName, other.attributeName);
  }

  @Override
  public int hashCode() {
    // Do not use value matcher here to support value wildcards
    return Objects.hash(attributeName);
  }

  @Override
  public String toString() {
    return attributeValue == null
        ? '{' + attributeName + '}'
        : '{' + attributeName + '=' + attributeValue + '}';
  }

  /**
   * Verify if this matcher is matching provided attribute value. If this matcher holds null value
   * then it is matching any attribute value
   *
   * @param value a value to be matched
   * @return true if this matcher is matching provided value, false otherwise.
   */
  boolean matchesValue(String value) {
    if ((attributeValue == null) || (value == null)) {
      return true;
    }
    return Objects.equals(attributeValue, value);
  }
}
