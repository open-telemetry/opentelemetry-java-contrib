/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static java.util.stream.Collectors.toList;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedConfigProvider;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesDurationUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

class InferredSpansConfig {

  private InferredSpansConfig() {}

  static final String ENABLED_OPTION = "enabled";

  static DeclarativeConfigProperties createDeclarativeConfig(ConfigProperties properties) {
    return ConfigPropertiesBackedConfigProvider.builder()
        .setAccessPath("", "otel.inferred.spans.")
        .build(properties)
        .getInstrumentationConfig();
  }

  static boolean isEnabled(DeclarativeConfigProperties properties) {
    return properties.getBoolean(ENABLED_OPTION, true);
  }

  static SpanProcessor createSpanProcessor(DeclarativeConfigProperties properties) {
    InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder().profilerEnabled(true);

    applyValue(properties.getBoolean("logging_enabled"), builder::profilerLoggingEnabled);

    applyValue(properties.getBoolean("backup_diagnostic_files"), builder::backupDiagnosticFiles);
    applyValue(properties.getInt("safe_mode"), builder::asyncProfilerSafeMode);
    applyValue(properties.getBoolean("post_processing_enabled"), builder::postProcessingEnabled);
    applyValue(
        DeclarativeConfigPropertiesDurationUtil.parseDuration(properties, "sampling_interval"),
        builder::samplingInterval);
    applyValue(
        DeclarativeConfigPropertiesDurationUtil.parseDuration(properties, "min_duration"),
        builder::inferredSpansMinDuration);
    applyWildcards(properties, "included_classes", builder::includedClasses);
    applyWildcards(properties, "excluded_classes", builder::excludedClasses);
    applyValue(
        DeclarativeConfigPropertiesDurationUtil.parseDuration(properties, "interval"),
        builder::profilerInterval);
    applyValue(
        DeclarativeConfigPropertiesDurationUtil.parseDuration(properties, "duration"),
        builder::profilingDuration);
    applyValue(properties.getString("lib_directory"), builder::profilerLibDirectory);

    String parentOverrideHandlerName = properties.getString("parent_override_handler");
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
