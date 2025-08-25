/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public final class StackTraceAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(StackTraceAutoConfig.class.getName());

  private static final String CONFIG_MIN_DURATION =
      "otel.java.experimental.span-stacktrace.min.duration";
  private static final Duration CONFIG_MIN_DURATION_DEFAULT = Duration.ofMillis(5);

  private static final String CONFIG_FILTER = "otel.java.experimental.span-stacktrace.filter";

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          long minDuration = getMinDuration(properties);
          if (minDuration >= 0) {
            Predicate<ReadableSpan> filter = getFilterPredicate(properties);
            providerBuilder.addSpanProcessor(new StackTraceSpanProcessor(minDuration, filter));
          }
          return providerBuilder;
        });
  }

  // package-private for testing
  static long getMinDuration(ConfigProperties properties) {
    long minDuration =
        properties.getDuration(CONFIG_MIN_DURATION, CONFIG_MIN_DURATION_DEFAULT).toNanos();
    if (minDuration < 0) {
      log.fine("Stack traces capture is disabled");
    } else {
      log.log(
          FINE,
          "Stack traces will be added to spans with a minimum duration of {0} nanos",
          minDuration);
    }
    return minDuration;
  }

  // package private for testing
  static Predicate<ReadableSpan> getFilterPredicate(ConfigProperties properties) {
    String filterClass = properties.getString(CONFIG_FILTER);
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
  @SuppressWarnings("unchecked")
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
