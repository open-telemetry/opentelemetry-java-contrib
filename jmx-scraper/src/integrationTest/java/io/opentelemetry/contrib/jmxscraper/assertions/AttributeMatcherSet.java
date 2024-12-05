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

  @Override
  public String toString() {
    return matchers.values().toString();
  }
}
