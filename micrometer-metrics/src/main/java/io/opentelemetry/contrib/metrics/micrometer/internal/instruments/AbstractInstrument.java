/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.RegisteredCallback;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

abstract class AbstractInstrument {
  private final InstrumentState instrumentState;
  private final Logger logger;

  protected AbstractInstrument(InstrumentState instrumentState) {
    this.instrumentState = instrumentState;
    this.logger = Logger.getLogger(getClass().getName());
  }

  protected final MeterRegistry meterRegistry() {
    return instrumentState.meterRegistry();
  }

  protected final String name() {
    return instrumentState.name();
  }

  @Nullable
  protected final String description() {
    return instrumentState.description();
  }

  @Nullable
  protected final String unit() {
    return instrumentState.unit();
  }

  @SuppressWarnings("PreferredInterfaceType")
  protected final Iterable<Tag> attributesToTags(Attributes attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return Collections.emptyList();
    }

    List<Tag> list = new ArrayList<>(attributes.size());
    attributes.forEach(
        (attributeKey, value) -> list.add(Tag.of(attributeKey.getKey(), Objects.toString(value))));
    return list;
  }

  protected final RegisteredCallback registerLongCallback(
      Consumer<ObservableLongMeasurement> callback, ObservableLongMeasurement measurement) {
    return registerCallback(() -> callback.accept(measurement));
  }

  protected final RegisteredCallback registerDoubleCallback(
      Consumer<ObservableDoubleMeasurement> callback, ObservableDoubleMeasurement measurement) {
    return registerCallback(() -> callback.accept(measurement));
  }

  protected final RegisteredCallback registerCallback(Runnable callback) {
    return instrumentState.registerCallback(invokeSafely(callback));
  }

  private Runnable invokeSafely(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Error error) {
        throw error;
      } catch (Throwable throwable) {
        logger.log(
            Level.WARNING,
            "An exception occurred invoking callback for instrument "
                + instrumentState.name()
                + ".",
            throwable);
      }
    };
  }
}
