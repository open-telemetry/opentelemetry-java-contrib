/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Holder class for a set of attribute matchers */
public class AttributeMatcherSet {

  // stored as a Map for easy lookup by name
  private final Map<String, AttributeMatcher> matchers;

  /**
   * Constructor for a set of attribute matchers
   *
   * @param matchers collection of matchers to build a set from
   * @throws IllegalStateException if there is any duplicate key
   */
  AttributeMatcherSet(Collection<AttributeMatcher> matchers) {
    this.matchers =
        matchers.stream().collect(Collectors.toMap(AttributeMatcher::getAttributeName, m -> m));
  }

  Map<String, AttributeMatcher> getMatchers() {
    return matchers;
  }

  /**
   * Checks if attributes match this attribute matcher set
   *
   * @param attributes attributes to check as map
   * @return {@literal true} when the attributes match all attributes from this set
   */
  public boolean matches(Map<String, String> attributes) {
    if(attributes.size() != matchers.size()) {
      return false;
    }

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      AttributeMatcher matcher = matchers.get(entry.getKey());
      if (matcher == null) {
        // no matcher for this key: unexpected key
        return false;
      }

      if (!matcher.matchesValue(entry.getValue())) {
        // value does not match: unexpected value
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return matchers.values().toString();
  }
}
