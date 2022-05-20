/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

public class DoubleMeasurement {
  private double value;

  public DoubleMeasurement(double value) {
    this.value = value;
  }

  public double value() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }
}
