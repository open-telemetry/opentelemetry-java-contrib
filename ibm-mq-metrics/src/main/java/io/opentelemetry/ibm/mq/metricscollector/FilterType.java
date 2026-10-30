/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

/** Types of name matching used by exclude filters. */
public enum FilterType {
  STARTSWITH,
  EQUALS,
  ENDSWITH,
  CONTAINS,
  NONE
}
