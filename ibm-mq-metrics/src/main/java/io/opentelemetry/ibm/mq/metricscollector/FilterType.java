/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

public enum FilterType {
  STARTSWITH,
  EQUALS,
  ENDSWITH,
  CONTAINS,
  NONE
}
