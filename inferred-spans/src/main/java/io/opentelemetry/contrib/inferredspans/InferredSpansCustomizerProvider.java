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
import java.util.Collections;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class InferredSpansCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          ConfigProperties configProperties =
              ConfigPropertiesUtil.resolveModel(
                  model, Collections.singletonMap("otel.inferred.spans", "inferred_spans"));

          return model;
        });
  }
}
