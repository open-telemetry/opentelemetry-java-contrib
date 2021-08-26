/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.List;

public class RuleBasedRoutingSamplerBuilder {
  private final List<SamplingRule> rules = new ArrayList<>();
  private final SpanKind kind;
  private final Sampler defaultDelegate;

  RuleBasedRoutingSamplerBuilder(SpanKind kind, Sampler defaultDelegate) {
    this.kind = kind;
    this.defaultDelegate = defaultDelegate;
  }

  public RuleBasedRoutingSamplerBuilder drop(AttributeKey<String> attributeKey, String pattern) {
    rules.add(new SamplingRule(attributeKey, pattern, Sampler.alwaysOff()));
    return this;
  }

  public RuleBasedRoutingSamplerBuilder recordAndSample(
      AttributeKey<String> attributeKey, String pattern) {
    rules.add(new SamplingRule(attributeKey, pattern, Sampler.alwaysOn()));
    return this;
  }

  public RuleBasedRoutingSampler build() {
    return new RuleBasedRoutingSampler(rules, kind, defaultDelegate);
  }
}
