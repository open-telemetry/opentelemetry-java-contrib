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

/** Helper interface representing any of the observable instruments. */
@FunctionalInterface
public interface CallbackRegistration
    extends ObservableLongCounter,
        ObservableDoubleCounter,
        ObservableLongUpDownCounter,
        ObservableDoubleUpDownCounter,
        ObservableLongGauge,
        ObservableDoubleGauge,
        AutoCloseable {

  /**
   * Remove the callback registered via {@code buildWithCallback(Consumer)}. After this is called,
   * the callback won't be invoked on future collections. Subsequent calls to {@link #close()} have
   * no effect.
   */
  @Override
  void close();
}
