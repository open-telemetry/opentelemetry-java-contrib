/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.ibm.mq.config;

import io.opentelemetry.ibm.mq.metricscollector.FilterType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExcludeFilters {

  private String type;
  private Set<String> values = new HashSet<>();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getValues() {
    return values;
  }

  public void setValues(Set<String> values) {
    this.values = values;
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
