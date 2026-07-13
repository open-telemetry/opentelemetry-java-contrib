/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedConfigProvider;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

class InferredSpansConfig {

  private InferredSpansConfig() {}

  static final String ENABLED_OPTION = "enabled";
  static final String LOGGING_OPTION = "logging_enabled";
  static final String DIAGNOSTIC_FILES_OPTION = "backup_diagnostic_files";
  static final String SAFEMODE_OPTION = "safe_mode";
  static final String POSTPROCESSING_OPTION = "post_processing_enabled";
  static final String SAMPLING_INTERVAL_OPTION = "sampling_interval";
  static final String MIN_DURATION_OPTION = "min_duration";
  static final String INCLUDED_CLASSES_OPTION = "included_classes";
  static final String EXCLUDED_CLASSES_OPTION = "excluded_classes";
  static final String INTERVAL_OPTION = "interval";
  static final String DURATION_OPTION = "duration";
  static final String LIB_DIRECTORY_OPTION = "lib_directory";
  static final String PARENT_OVERRIDE_HANDLER_OPTION = "parent_override_handler";

  static final List<String> ALL_PROPERTIES =
      unmodifiableList(
          Arrays.asList(
              ENABLED_OPTION,
              LOGGING_OPTION,
              DIAGNOSTIC_FILES_OPTION,
              SAFEMODE_OPTION,
              POSTPROCESSING_OPTION,
              SAMPLING_INTERVAL_OPTION,
              MIN_DURATION_OPTION,
              INCLUDED_CLASSES_OPTION,
              EXCLUDED_CLASSES_OPTION,
              INTERVAL_OPTION,
              DURATION_OPTION,
              LIB_DIRECTORY_OPTION,
              PARENT_OVERRIDE_HANDLER_OPTION));

  private static final String PREFIX = "otel.inferred.spans.";

  static DeclarativeConfigProperties createDeclarativeConfig(ConfigProperties properties) {
    ConfigPropertiesBackedConfigProvider.Builder builder =
        ConfigPropertiesBackedConfigProvider.builder();
    for (String property : ALL_PROPERTIES) {
      addConfigMapping(builder, property);
    }
    return builder.build(properties).getInstrumentationConfig();
  }

  private static void addConfigMapping(
      ConfigPropertiesBackedConfigProvider.Builder builder, String configProperty) {
    builder.addMapping(configProperty, toSystemProperty(configProperty));
  }

  static boolean isEnabled(DeclarativeConfigProperties properties) {
    return properties.getBoolean(ENABLED_OPTION, true);
  }

  static String toSystemProperty(String configProperty) {
    return PREFIX + configProperty.replace("_", ".");
  }

  static SpanProcessor createSpanProcessor(DeclarativeConfigProperties properties) {
    InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder().profilerEnabled(true);

    applyValue(properties.getBoolean(LOGGING_OPTION), builder::profilerLoggingEnabled);

    applyValue(properties.getBoolean(DIAGNOSTIC_FILES_OPTION), builder::backupDiagnosticFiles);
    applyValue(properties.getInt(SAFEMODE_OPTION), builder::asyncProfilerSafeMode);
    applyValue(properties.getBoolean(POSTPROCESSING_OPTION), builder::postProcessingEnabled);
    applyValue(parseDuration(properties, SAMPLING_INTERVAL_OPTION), builder::samplingInterval);
    applyValue(parseDuration(properties, MIN_DURATION_OPTION), builder::inferredSpansMinDuration);
    applyWildcards(properties, INCLUDED_CLASSES_OPTION, builder::includedClasses);
    applyWildcards(properties, EXCLUDED_CLASSES_OPTION, builder::excludedClasses);
    applyValue(parseDuration(properties, INTERVAL_OPTION), builder::profilerInterval);
    applyValue(parseDuration(properties, DURATION_OPTION), builder::profilingDuration);
    applyValue(properties.getString(LIB_DIRECTORY_OPTION), builder::profilerLibDirectory);

    String parentOverrideHandlerName = properties.getString(PARENT_OVERRIDE_HANDLER_OPTION);
    if (parentOverrideHandlerName != null && !parentOverrideHandlerName.isEmpty()) {
      builder.parentOverrideHandler(constructParentOverrideHandler(parentOverrideHandlerName));
    }

    return builder.build();
  }

  @SuppressWarnings("unchecked") // handler must implement BiConsumer<SpanBuilder, SpanContext>
  private static BiConsumer<SpanBuilder, SpanContext> constructParentOverrideHandler(String name) {
    try {
      Class<?> clazz = Class.forName(name);
      return (BiConsumer<SpanBuilder, SpanContext>) clazz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not construct parent override handler", e);
    }
  }

  private static <T> void applyValue(@Nullable T value, Consumer<T> funcToApply) {
    if (value != null) {
      funcToApply.accept(value);
    }
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

  private static void applyWildcards(
      DeclarativeConfigProperties properties,
      String key,
      Consumer<? super List<WildcardMatcher>> funcToApply) {
    String wildcardListString = properties.getString(key);
    if (wildcardListString != null && !wildcardListString.isEmpty()) {
      List<WildcardMatcher> values =
          Arrays.stream(wildcardListString.split(","))
              .filter(str -> !str.isEmpty())
              .map(WildcardMatcher::valueOf)
              .collect(toList());
      if (!values.isEmpty()) {
        funcToApply.accept(values);
      }
    }
  }
}
