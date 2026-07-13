/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static java.util.Collections.singletonMap;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedConfigProvider;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class StackTraceAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(StackTraceAutoConfig.class.getName());

  static final String PREFIX = "otel.java.experimental.span-stacktrace.";
  static final String CONFIG_MIN_DURATION = PREFIX + "min.duration";
  private static final Duration CONFIG_MIN_DURATION_DEFAULT = Duration.ofMillis(5);
  private static final String CONFIG_FILTER = PREFIX + "filter";

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          DeclarativeConfigProperties declarativeConfig = createDeclarativeConfig(properties);
          if (getMinDuration(declarativeConfig) >= 0) {
            providerBuilder.addSpanProcessor(create(declarativeConfig));
          }
          return providerBuilder;
        });
  }

  static StackTraceSpanProcessor create(DeclarativeConfigProperties properties) {
    return new StackTraceSpanProcessor(getMinDuration(properties), getFilterPredicate(properties));
  }

  static DeclarativeConfigProperties createDeclarativeConfig(ConfigProperties properties) {
    return ConfigPropertiesBackedConfigProvider.builder()
        .addMapping("min_duration", CONFIG_MIN_DURATION)
        .addMapping("filter", CONFIG_FILTER)
        .build(properties)
        .getInstrumentationConfig();
  }

  // package-private for testing
  static long getMinDuration(DeclarativeConfigProperties properties) {
    Duration minDuration = parseDuration(properties, "min_duration");
    if (minDuration == null) {
      minDuration = CONFIG_MIN_DURATION_DEFAULT;
    }
    long minDurationNanos = minDuration.toNanos();
    if (minDurationNanos < 0) {
      log.fine("Stack traces capture is disabled");
    } else {
      log.log(
          FINE,
          "Stack traces will be added to spans with a minimum duration of {0} nanos",
          minDurationNanos);
    }
    return minDurationNanos;
  }

  @Nullable
  private static Duration parseDuration(DeclarativeConfigProperties properties, String key) {
    if (properties instanceof ConfigPropertiesBackedDeclarativeConfigProperties) {
      String rawValue = properties.getString(key);
      if (rawValue == null || rawValue.isEmpty()) {
        return null;
      }
      return DefaultConfigProperties.createFromMap(singletonMap(key, rawValue)).getDuration(key);
    }

    Long rawLongValue = properties.getLong(key);
    if (rawLongValue == null) {
      return null;
    }
    return Duration.ofMillis(rawLongValue);
  }

  static Predicate<ReadableSpan> getFilterPredicate(DeclarativeConfigProperties properties) {
    return getFilterPredicate(properties.getString("filter"));
  }

  private static Predicate<ReadableSpan> getFilterPredicate(@Nullable String filterClass) {
    Predicate<ReadableSpan> filter = null;
    if (filterClass != null) {
      Class<?> filterType = getFilterType(filterClass);
      if (filterType != null) {
        filter = getFilterInstance(filterType);
      }
    }

    if (filter == null) {
      // if value is set, lack of filtering is likely an error and must be reported
      Level disabledLogLevel = filterClass != null ? SEVERE : FINE;
      log.log(disabledLogLevel, "Span stacktrace filtering disabled");
      return span -> true;
    } else {
      log.fine("Span stacktrace filtering enabled with: " + filterClass);
      return filter;
    }
  }

  @Nullable
  private static Class<?> getFilterType(String filterClass) {
    try {
      Class<?> filterType = Class.forName(filterClass);
      if (!Predicate.class.isAssignableFrom(filterType)) {
        log.severe("Filter must be a subclass of java.util.function.Predicate");
        return null;
      }
      return filterType;
    } catch (ClassNotFoundException e) {
      log.severe("Unable to load filter class: " + filterClass);
      return null;
    }
  }

  @Nullable
  @SuppressWarnings("unchecked") // filterType must implement Predicate<ReadableSpan>
  private static Predicate<ReadableSpan> getFilterInstance(Class<?> filterType) {
    try {
      Constructor<?> constructor = filterType.getConstructor();
      return (Predicate<ReadableSpan>) constructor.newInstance();
    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      log.severe("Unable to create filter instance with no-arg constructor: " + filterType);
      return null;
    }
  }
}
