/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
  @Nullable private final Set<AttributeKey<?>> attributeKeySet;
  private final Map<Attributes, Iterable<Tag>> attributesTagsCache;

  protected AbstractInstrument(InstrumentState instrumentState) {
    this.instrumentState = instrumentState;
    this.logger = Logger.getLogger(getClass().getName());
    this.instrumentationNameTag = instrumentState.instrumentationScopeNameTag();
    this.instrumentationVersionTag = instrumentState.instrumentationScopeVersionTag();
    List<AttributeKey<?>> attributes = instrumentState.attributesAdvice();
    if (attributes != null) {
      attributeKeySet = new HashSet<>(attributes);
    } else {
      attributeKeySet = null;
    }
    this.attributesTagsCache = new ConcurrentHashMap<>();
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

  protected final Attributes effectiveAttributes(@Nullable Attributes attributes) {
    if (attributes == null) {
      return Attributes.empty();
    }
    if (attributeKeySet == null) {
      return attributes;
    }
    return attributes.toBuilder().removeIf(key -> !attributeKeySet.contains(key)).build();
  }

  @SuppressWarnings("PreferredInterfaceType")
  protected final Iterable<Tag> attributesToTags(Attributes attributes) {
    return attributesTagsCache.computeIfAbsent(
        effectiveAttributes(attributes), this::calculateTags);
  }

  @SuppressWarnings("PreferredInterfaceType")
  private Iterable<Tag> calculateTags(Attributes attributes) {
    Attributes effectiveAttributes = effectiveAttributes(attributes);
    List<Tag> list = new ArrayList<>(effectiveAttributes.size() + 2);
    effectiveAttributes.forEach(
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
