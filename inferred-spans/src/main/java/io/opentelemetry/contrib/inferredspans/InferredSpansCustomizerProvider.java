/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.Collections;
import java.util.Map;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class InferredSpansCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  static final Map<String, String> TRANSLATION_MAP =
      Collections.singletonMap("otel.inferred.spans", "inferred_spans");

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          ConfigProperties configProperties =
              ConfigPropertiesUtil.resolveModel(model, TRANSLATION_MAP);

          TracerProviderModel tracerProvider = model.getTracerProvider();
          if (tracerProvider != null && InferredSpansAutoConfig.isEnabled(configProperties)) {
            tracerProvider.getProcessors().add(create());
          }

          return model;
        });
  }

  @SuppressWarnings("NullAway")
  private static SpanProcessorModel create() {
    return new SpanProcessorModel().withAdditionalProperty("inferred_spans", null);
  }
}
