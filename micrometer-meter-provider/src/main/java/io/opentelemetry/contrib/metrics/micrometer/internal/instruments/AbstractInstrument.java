/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static io.opentelemetry.contrib.metrics.micrometer.internal.instruments.Constants.INSTRUMENTATION_NAME;
import static io.opentelemetry.contrib.metrics.micrometer.internal.instruments.Constants.INSTRUMENTATION_VERSION;
import static io.opentelemetry.contrib.metrics.micrometer.internal.instruments.Constants.UNKNOWN;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.util.StringUtils;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

abstract class AbstractInstrument {
  private final InstrumentState instrumentState;
  private final Logger logger;
  private final Tag instrumentationNameTag;
  private final Tag instrumentationVersionTag;
  private final Map<Attributes, Iterable<Tag>> attributesTagsCache;

  protected AbstractInstrument(InstrumentState instrumentState) {
    this.instrumentState = instrumentState;
    this.logger = Logger.getLogger(getClass().getName());
    instrumentationNameTag =
        instrumentationScopeNameTag(instrumentState.instrumentationScopeName());
    instrumentationVersionTag =
        instrumentationScopeVersionTag(instrumentState.instrumentationScopeVersion());
    this.attributesTagsCache = new ConcurrentHashMap<>();
  }

  private static Tag instrumentationScopeNameTag(String instrumentationScopeName) {
    return Tag.of(INSTRUMENTATION_NAME, instrumentationScopeName);
  }

  private static Tag instrumentationScopeVersionTag(@Nullable String instrumentationScopeVersion) {
    if (StringUtils.isBlank(instrumentationScopeVersion)) {
      instrumentationScopeVersion = UNKNOWN;
    }
    return Tag.of(INSTRUMENTATION_VERSION, instrumentationScopeVersion);
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

  protected final Attributes attributesOrEmpty(@Nullable Attributes attributes) {
    return attributes != null ? attributes : Attributes.empty();
  }

  @SuppressWarnings("PreferredInterfaceType")
  protected final Iterable<Tag> attributesToTags(Attributes attributes) {
    return attributesTagsCache.computeIfAbsent(attributesOrEmpty(attributes), this::calculateTags);
  }

  @SuppressWarnings("PreferredInterfaceType")
  private Iterable<Tag> calculateTags(Attributes attributes) {
    List<Tag> list = new ArrayList<>(attributes.size() + 2);
    attributes.forEach(
        (attributeKey, value) -> list.add(Tag.of(attributeKey.getKey(), Objects.toString(value))));

    list.add(instrumentationNameTag);
    list.add(instrumentationVersionTag);
    return Collections.unmodifiableList(list);
  }

  protected final CallbackRegistration registerLongCallback(
      Consumer<ObservableLongMeasurement> callback, ObservableLongMeasurement measurement) {
    return registerCallback(() -> callback.accept(measurement));
  }

  protected final CallbackRegistration registerDoubleCallback(
      Consumer<ObservableDoubleMeasurement> callback, ObservableDoubleMeasurement measurement) {
    return registerCallback(() -> callback.accept(measurement));
  }

  protected final CallbackRegistration registerCallback(Runnable callback) {
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
