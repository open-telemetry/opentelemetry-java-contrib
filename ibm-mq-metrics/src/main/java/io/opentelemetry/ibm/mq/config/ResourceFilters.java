/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Include and exclude filters for IBM MQ resource names. */
public final class ResourceFilters {

  private Set<String> include = new HashSet<>();
  private List<ExcludeFilters> exclude = new ArrayList<>();

  public Set<String> getInclude() {
    return include;
  }

  public void setInclude(Set<String> include) {
    this.include = include;
  }

  public List<ExcludeFilters> getExclude() {
    return exclude;
  }

  public void setExclude(List<ExcludeFilters> exclude) {
    this.exclude = exclude;
  }
}
