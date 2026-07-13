/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedConfigProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

class InferredSpansConfig {

  private InferredSpansConfig() {}

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
      builder.addMapping(translateDeclarativeKey(property), property);
    }
    return builder.build(properties).getInstrumentationConfig();
  }

  static boolean isEnabled(DeclarativeConfigProperties properties) {
    Boolean enabled = properties.getBoolean(translateDeclarativeKey(ENABLED_OPTION));
    return enabled == null || enabled;
  }

  static SpanProcessor createSpanProcessor(DeclarativeConfigProperties properties) {
    InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder().profilerEnabled(true);

    DeclarativeConfigPropertiesApplier applier = new DeclarativeConfigPropertiesApplier(properties);

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

    String parentOverrideHandlerName =
        properties.getString(translateDeclarativeKey(PARENT_OVERRIDE_HANDLER_OPTION));
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

  private static String translateDeclarativeKey(String configKey) {
    return configKey.substring(PREFIX.length()).replace('.', '_');
  }

  private static <T> void applyValue(@Nullable T value, Consumer<T> funcToApply) {
    if (value != null) {
      funcToApply.accept(value);
    }
  }

  private static TimeUnit getDurationUnit(String unitString) {
    switch (unitString) {
      case "us":
        return TimeUnit.MICROSECONDS;
      case "ns":
        return TimeUnit.NANOSECONDS;
      case "":
      case "ms":
        return TimeUnit.MILLISECONDS;
      case "s":
        return TimeUnit.SECONDS;
      case "m":
        return TimeUnit.MINUTES;
      case "h":
        return TimeUnit.HOURS;
      case "d":
        return TimeUnit.DAYS;
      default:
        throw new ConfigurationException("Invalid duration string, found: " + unitString);
    }
  }

  private static String getUnitString(String rawValue) {
    int lastDigitIndex = rawValue.length() - 1;
    while (lastDigitIndex >= 0) {
      char c = rawValue.charAt(lastDigitIndex);
      if (Character.isDigit(c)) {
        break;
      }
      lastDigitIndex -= 1;
    }
    return rawValue.substring(lastDigitIndex + 1);
  }

  private static class DeclarativeConfigPropertiesApplier {

    private final DeclarativeConfigProperties properties;

    private DeclarativeConfigPropertiesApplier(DeclarativeConfigProperties properties) {
      this.properties = properties;
    }

    void applyBool(String configKey, Consumer<Boolean> funcToApply) {
      applyValue(properties.getBoolean(translateDeclarativeKey(configKey)), funcToApply);
    }

    void applyInt(String configKey, Consumer<Integer> funcToApply) {
      applyValue(properties.getInt(translateDeclarativeKey(configKey)), funcToApply);
    }

    void applyDuration(String configKey, Consumer<Duration> funcToApply) {
      applyValue(parseDuration(configKey), funcToApply);
    }

    @Nullable
    private Duration parseDuration(String configKey) {
      String rawValue = properties.getString(translateDeclarativeKey(configKey));
      if (rawValue == null || rawValue.isEmpty()) {
        return null;
      }
      String unitString = getUnitString(rawValue);
      String numberString = rawValue.substring(0, rawValue.length() - unitString.length());
      try {
        long rawNumber = Long.parseLong(numberString.trim());
        TimeUnit unit = getDurationUnit(unitString.trim());
        return Duration.ofNanos(TimeUnit.NANOSECONDS.convert(rawNumber, unit));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(
            "Invalid duration property "
                + translateDeclarativeKey(configKey)
                + "="
                + rawValue
                + ". Expected number, found: "
                + numberString,
            e);
      } catch (ConfigurationException e) {
        throw new ConfigurationException(
            "Invalid duration property "
                + translateDeclarativeKey(configKey)
                + "="
                + rawValue
                + ". "
                + e.getMessage());
      }
    }

    void applyString(String configKey, Consumer<String> funcToApply) {
      applyValue(properties.getString(translateDeclarativeKey(configKey)), funcToApply);
    }

    void applyWildcards(String configKey, Consumer<? super List<WildcardMatcher>> funcToApply) {
      String wildcardListString = properties.getString(translateDeclarativeKey(configKey));
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
}
