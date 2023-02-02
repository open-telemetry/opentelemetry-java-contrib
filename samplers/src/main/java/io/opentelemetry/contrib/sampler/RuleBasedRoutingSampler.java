/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/**
 * This sampler accepts a list of {@link SamplingRule}s and tries to match every proposed span
 * against those rules. Every rule describes a span's attribute, a pattern against which to match
 * attribute's value, and a sampler that will make a decision about given span if match was
 * successful.
 *
 * <p>Matching is performed by {@link java.util.regex.Pattern}.
 *
 * <p>Provided span kind is checked first and if differs from the one given to {@link
 * #builder(SpanKind, Sampler)}, the default fallback sampler will make a decision.
 *
 * <p>Note that only attributes that were set on {@link io.opentelemetry.api.trace.SpanBuilder} will
 * be taken into account, attributes set after the span has been started are not used
 *
 * <p>If none of the rules matched, the default fallback sampler will make a decision.
 */
public final class RuleBasedRoutingSampler implements Sampler {
  private final List<SamplingRule> rules;
  private final SpanKind kind;
  private final Sampler fallback;

  RuleBasedRoutingSampler(List<SamplingRule> rules, SpanKind kind, Sampler fallback) {
    this.kind = requireNonNull(kind);
    this.fallback = requireNonNull(fallback);
    this.rules = requireNonNull(rules);
  }

  public static RuleBasedRoutingSamplerBuilder builder(SpanKind kind, Sampler fallback) {
    return new RuleBasedRoutingSamplerBuilder(
        requireNonNull(kind, "span kind must not be null"),
        requireNonNull(fallback, "fallback sampler must not be null"));
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
    return "RuleBasedRoutingSampler{"
        + "rules="
        + rules
        + ", kind="
        + kind
        + ", fallback="
        + fallback
        + '}';
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
