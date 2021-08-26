/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Objects;

/**
 * This sampler accepts a list of {@link SamplingRule}s and tries to match every proposed spans
 * against those rules. Every rule describes a span's attribute, a pattern against which to match
 * attribute's value, and a sampler that will make a decision about given span if match was
 * successful.
 *
 * <p>If none of the rules matched, the default falback sampler will make a decision.
 */
public class RuleBasedRoutingSampler implements Sampler {
  private final List<SamplingRule> rules;
  private final SpanKind kind;
  private final Sampler fallback;

  RuleBasedRoutingSampler(List<SamplingRule> rules, SpanKind kind, Sampler fallback) {
    this.kind = Objects.requireNonNull(kind);
    this.fallback = Objects.requireNonNull(fallback);
    this.rules = Objects.requireNonNull(rules);
  }

  public RuleBasedRoutingSamplerBuilder builder(SpanKind kind, Sampler fallback) {
    return new RuleBasedRoutingSamplerBuilder(kind, fallback);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    if (kind != spanKind) {
      return fallback.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    for (SamplingRule samplingRule : rules) {
      String attributeValue = attributes.get(samplingRule.attributeKey);
      if (attributeValue == null) {
        continue;
      }
      if (samplingRule.pattern.matcher(attributeValue).find()) {
        return samplingRule.delegate.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);
      }
    }
    return fallback.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return toString();
  }

  @Override
  public String toString() {
    return "RuleBasedRoutingSampler{"
        + "rules="
        + rules
        + ", kind="
        + kind
        + ", fallback="
        + fallback
        + '}';
  }
}
