/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansAutoConfig.class.getName());

  static final String ENABLED_OPTION = "otel.inferred.spans.enabled";
  static final String LOGGING_OPTION = "otel.inferred.spans.logging.enabled";
  static final String DIAGNOSTIC_FILES_OPTION = "otel.inferred.spans.backup.diagnostic.files";
  static final String SAFEMODE_OPTION = "otel.inferred.spans.safe.mode";
  static final String POSTPROCESSING_OPTION = "otel.inferred.spans.post.processing.enabled";
  static final String SAMPLING_INTERVAL_OPTION = "otel.inferred.spans.sampling.interval";
  static final String MIN_DURATION_OPTION = "otel.inferred.spans.min.duration";
  static final String INCLUDED_CLASSES_OPTION = "otel.inferred.spans.included.classes";
  static final String EXCLUDED_CLASSES_OPTION = "otel.inferred.spans.excluded.classes";
  static final String INTERVAL_OPTION = "otel.inferred.spans.interval";
  static final String DURATION_OPTION = "otel.inferred.spans.duration";
  static final String LIB_DIRECTORY_OPTION = "otel.inferred.spans.lib.directory";
  static final String PARENT_OVERRIDE_HANDLER_OPTION =
      "otel.inferred.spans.parent.override.handler";

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          if (properties.getBoolean(ENABLED_OPTION, false)) {
            providerBuilder.addSpanProcessor(create(properties));
          } else {
            log.finest(
                "Not enabling inferred spans processor because " + ENABLED_OPTION + " is not set");
          }
          return providerBuilder;
        });
  }

  static SpanProcessor create(ConfigProperties properties) {
    InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder();

    PropertiesApplier applier = new PropertiesApplier(properties);

    applier.applyBool(LOGGING_OPTION, builder::profilerLoggingEnabled);
    applier.applyBool(DIAGNOSTIC_FILES_OPTION, builder::backupDiagnosticFiles);
    applier.applyInt(SAFEMODE_OPTION, builder::asyncProfilerSafeMode);
    applier.applyBool(POSTPROCESSING_OPTION, builder::postProcessingEnabled);
    applier.applyDuration(SAMPLING_INTERVAL_OPTION, builder::samplingInterval);
    applier.applyDuration(MIN_DURATION_OPTION, builder::inferredSpansMinDuration);
    applier.applyWildcards(INCLUDED_CLASSES_OPTION, builder::includedClasses);
    applier.applyWildcards(EXCLUDED_CLASSES_OPTION, builder::excludedClasses);
    applier.applyDuration(INTERVAL_OPTION, builder::profilerInterval);
    applier.applyDuration(DURATION_OPTION, builder::profilingDuration);
    applier.applyString(LIB_DIRECTORY_OPTION, builder::profilerLibDirectory);

    String parentOverrideHandlerName = properties.getString(PARENT_OVERRIDE_HANDLER_OPTION);
    if (parentOverrideHandlerName != null && !parentOverrideHandlerName.isEmpty()) {
      builder.parentOverrideHandler(constructParentOverrideHandler(parentOverrideHandlerName));
    }

    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static BiConsumer<SpanBuilder, SpanContext> constructParentOverrideHandler(String name) {
    try {
      Class<?> clazz = Class.forName(name);
      return (BiConsumer<SpanBuilder, SpanContext>) clazz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not construct parent override handler", e);
    }
  }

  private static class PropertiesApplier {

    private final ConfigProperties properties;

    PropertiesApplier(ConfigProperties properties) {
      this.properties = properties;
    }

    void applyBool(String configKey, Consumer<Boolean> funcToApply) {
      applyValue(properties.getBoolean(configKey), funcToApply);
    }

    void applyInt(String configKey, Consumer<Integer> funcToApply) {
      applyValue(properties.getInt(configKey), funcToApply);
    }

    void applyDuration(String configKey, Consumer<Duration> funcToApply) {
      applyValue(properties.getDuration(configKey), funcToApply);
    }

    void applyString(String configKey, Consumer<String> funcToApply) {
      applyValue(properties.getString(configKey), funcToApply);
    }

    void applyWildcards(String configKey, Consumer<? super List<WildcardMatcher>> funcToApply) {
      String wildcardListString = properties.getString(configKey);
      if (wildcardListString != null && !wildcardListString.isEmpty()) {
        List<WildcardMatcher> values =
            Arrays.stream(wildcardListString.split(","))
                .filter(str -> !str.isEmpty())
                .map(WildcardMatcher::valueOf)
                .collect(Collectors.toList());
        if (!values.isEmpty()) {
          funcToApply.accept(values);
        }
      }
    }

    private static <T> void applyValue(@Nullable T value, Consumer<T> funcToApply) {
      if (value != null) {
        funcToApply.accept(value);
      }
    }
  }
}
