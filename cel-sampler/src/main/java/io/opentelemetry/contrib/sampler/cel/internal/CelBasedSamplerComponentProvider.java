/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel.internal;

import dev.cel.common.CelValidationException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.sampler.cel.CelBasedSampler;
import io.opentelemetry.contrib.sampler.cel.CelBasedSamplerBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;

/**
 * Declarative configuration SPI implementation for {@link CelBasedSampler}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class CelBasedSamplerComponentProvider implements ComponentProvider<Sampler> {

  private static final String ACTION_RECORD_AND_SAMPLE = "RECORD_AND_SAMPLE";
  private static final String ACTION_DROP = "DROP";

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "cel_based";
  }

  @Override
  public Sampler create(DeclarativeConfigProperties config) {
    List<DeclarativeConfigProperties> expressions = config.getStructuredList("expressions");
    if (expressions == null || expressions.isEmpty()) {
      throw new DeclarativeConfigException("cel_based sampler .expressions is required");
    }

    CelBasedSamplerBuilder builder = CelBasedSampler.builder(getFallbackSampler(config));

    for (DeclarativeConfigProperties expressionConfig : expressions) {
      String expression = expressionConfig.getString("expression");
      if (expression == null) {
        throw new DeclarativeConfigException(
            "cel_based sampler .expressions[].expression is required");
      }

      String action = expressionConfig.getString("action");
      if (action == null) {
        throw new DeclarativeConfigException("cel_based sampler .expressions[].action is required");
      }

      try {
        if (action.equals(ACTION_RECORD_AND_SAMPLE)) {
          builder.recordAndSample(expression);
        } else if (action.equals(ACTION_DROP)) {
          builder.drop(expression);
        } else {
          throw new DeclarativeConfigException(
              "cel_based sampler .expressions[].action must be "
                  + ACTION_RECORD_AND_SAMPLE
                  + " or "
                  + ACTION_DROP);
        }
      } catch (CelValidationException e) {
        throw new DeclarativeConfigException(
            "Failed to compile CEL expression: '" + expression + "'. CEL error: " + e.getMessage(),
            e);
      }
    }
    return builder.build();
  }

  private static Sampler getFallbackSampler(DeclarativeConfigProperties config) {
    DeclarativeConfigProperties fallbackModel = config.getStructured("fallback_sampler");
    if (fallbackModel == null) {
      throw new DeclarativeConfigException(
          "cel_based sampler .fallback_sampler is required but is null");
    }
    Sampler fallbackSampler;
    try {
      fallbackSampler = DeclarativeConfiguration.createSampler(fallbackModel);
    } catch (DeclarativeConfigException e) {
      throw new DeclarativeConfigException(
          "cel_based sampler failed to create .fallback_sampler sampler", e);
    }
    return fallbackSampler;
  }
}
