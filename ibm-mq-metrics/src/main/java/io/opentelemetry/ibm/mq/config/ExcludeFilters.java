/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.config;

import io.opentelemetry.ibm.mq.metricscollector.FilterType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A jackson databind class used for config.
 */
public class ExcludeFilters {

  private String type = "UNKNOWN";
  private Set<String> values = new HashSet<>();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getValues() {
    return Collections.unmodifiableSet(values);
  }

  public void setValues(Set<String> values) {
    this.values = new HashSet<>(values);
  }

  public static boolean isExcluded(String resourceName, Collection<ExcludeFilters> excludeFilters) {
    if (excludeFilters == null) {
      return false;
    }
    for (ExcludeFilters filter : excludeFilters) {
      if (filter.isExcluded(resourceName)) {
        return true;
      }
    }
    return false;
  }

  public boolean isExcluded(String resourceName) {
    if (resourceName == null || resourceName.isEmpty()) {
      return true;
    }
    switch (FilterType.valueOf(type)) {
      case CONTAINS:
        for (String filterValue : values) {
          if (resourceName.contains(filterValue)) {
            return true;
          }
        }
        break;
      case STARTSWITH:
        for (String filterValue : values) {
          if (resourceName.startsWith(filterValue)) {
            return true;
          }
        }
        break;
      case NONE:
        return false;
      case EQUALS:
        for (String filterValue : values) {
          if (resourceName.equals(filterValue)) {
            return true;
          }
        }
        break;
      case ENDSWITH:
        for (String filterValue : values) {
          if (resourceName.endsWith(filterValue)) {
            return true;
          }
        }
    }
    return false;
  }
}
