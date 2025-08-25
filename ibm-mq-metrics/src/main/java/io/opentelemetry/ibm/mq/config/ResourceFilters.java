/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.config;

import java.util.HashSet;
import java.util.Set;

public final class ResourceFilters {

  private Set<String> include = new HashSet<>();
  private Set<ExcludeFilters> exclude = new HashSet<>();

  public Set<String> getInclude() {
    return include;
  }

  public void setInclude(Set<String> include) {
    this.include = include;
  }

  public Set<ExcludeFilters> getExclude() {
    return exclude;
  }

  public void setExclude(Set<ExcludeFilters> exclude) {
    this.exclude = exclude;
  }
}
