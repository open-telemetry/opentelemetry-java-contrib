/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

public class LongMeasurement {
  private long value;

  public LongMeasurement(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }

  public void setValue(long value) {
    this.value = value;
  }
}
