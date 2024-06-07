/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.contrib.inferredspans.config.PropertiesApplier;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansAutoConfig.class.getName());

  static final String ENABLED_OPTION = "elastic.otel.inferred.spans.enabled";
  static final String LOGGING_OPTION = "elastic.otel.inferred.spans.logging.enabled";
  static final String DIAGNOSTIC_FILES_OPTION =
      "elastic.otel.inferred.spans.backup.diagnostic.files";
  static final String SAFEMODE_OPTION = "elastic.otel.inferred.spans.safe.mode";
  static final String POSTPROCESSING_OPTION = "elastic.otel.inferred.spans.post.processing.enabled";
  static final String SAMPLING_INTERVAL_OPTION = "elastic.otel.inferred.spans.sampling.interval";
  static final String MIN_DURATION_OPTION = "elastic.otel.inferred.spans.min.duration";
  static final String INCLUDED_CLASSES_OPTION = "elastic.otel.inferred.spans.included.classes";
  static final String EXCLUDED_CLASSES_OPTION = "elastic.otel.inferred.spans.excluded.classes";
  static final String INTERVAL_OPTION = "elastic.otel.inferred.spans.interval";
  static final String DURATION_OPTION = "elastic.otel.inferred.spans.duration";
  static final String LIB_DIRECTORY_OPTION = "elastic.otel.inferred.spans.lib.directory";

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          if (properties.getBoolean(ENABLED_OPTION, false)) {
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

            providerBuilder.addSpanProcessor(builder.build());
          } else {
            log.finest(
                "Not enabling inferred spans processor because " + ENABLED_OPTION + " is not set");
          }
          return providerBuilder;
        });
  }
}
