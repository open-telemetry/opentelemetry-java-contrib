/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.internal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.StructuredConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.FileConfiguration;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;

/**
 * Declarative configuration SPI implementation for {@link RuleBasedRoutingSampler}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class RuleBasedRoutingSamplerComponentProvider implements ComponentProvider<Sampler> {

  private static final String ACTION_RECORD_AND_SAMPLE = "RECORD_AND_SAMPLE";
  private static final String ACTION_DROP = "DROP";

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "rule_based_routing";
  }

  @Override
  public Sampler create(StructuredConfigProperties config) {
    StructuredConfigProperties fallbackModel = config.getStructured("fallback_sampler");
    if (fallbackModel == null) {
      throw new ConfigurationException(
          "rule_based_routing sampler .fallback is required but is null");
    }
    Sampler fallbackSampler;
    try {
      fallbackSampler = FileConfiguration.createSampler(fallbackModel);
    } catch (ConfigurationException e) {
      throw new ConfigurationException(
          "rule_Based_routing sampler failed to create .fallback sampler", e);
    }

    String spanKindString = config.getString("span_kind", "SERVER");
    SpanKind spanKind;
    try {
      spanKind = SpanKind.valueOf(spanKindString);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException(
          "rule_based_routing sampler .span_kind is invalid: " + spanKindString, e);
    }

    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(spanKind, fallbackSampler);

    List<StructuredConfigProperties> rules = config.getStructuredList("rules");
    if (rules == null || rules.isEmpty()) {
      throw new ConfigurationException("rule_based_routing sampler .rules is required");
    }

    for (StructuredConfigProperties rule : rules) {
      String attribute = rule.getString("attribute");
      if (attribute == null) {
        throw new ConfigurationException(
            "rule_based_routing sampler .rules[].attribute is required");
      }
      AttributeKey<String> attributeKey = AttributeKey.stringKey(attribute);
      String pattern = rule.getString("pattern");
      if (pattern == null) {
        throw new ConfigurationException("rule_based_routing sampler .rules[].pattern is required");
      }
      String action = rule.getString("action");
      if (action == null) {
        throw new ConfigurationException("rule_based_routing sampler .rules[].action is required");
      }
      if (action.equals(ACTION_RECORD_AND_SAMPLE)) {
        builder.recordAndSample(attributeKey, pattern);
      } else if (action.equals(ACTION_DROP)) {
        builder.drop(attributeKey, pattern);
      } else {
        throw new ConfigurationException(
            "rule_based_routing sampler .rules[].action is must be "
                + ACTION_RECORD_AND_SAMPLE
                + " or "
                + ACTION_DROP);
      }
    }

    return builder.build();
  }
}
