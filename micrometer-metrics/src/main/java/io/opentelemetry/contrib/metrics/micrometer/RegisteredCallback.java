/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;

@FunctionalInterface
public interface RegisteredCallback
    extends ObservableLongCounter,
        ObservableDoubleCounter,
        ObservableLongUpDownCounter,
        ObservableDoubleUpDownCounter,
        ObservableLongGauge,
        ObservableDoubleGauge {

  @Override
  void close();
}
