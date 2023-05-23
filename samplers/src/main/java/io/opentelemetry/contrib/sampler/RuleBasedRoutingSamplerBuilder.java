/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.List;

public final class RuleBasedRoutingSamplerBuilder {
  private final List<SamplingRule> rules = new ArrayList<>();
  private final SpanKind kind;
  private final Sampler defaultDelegate;

  RuleBasedRoutingSamplerBuilder(SpanKind kind, Sampler defaultDelegate) {
    this.kind = kind;
    this.defaultDelegate = defaultDelegate;
  }

  /**
   * Drop all spans when the value of the provided {@link AttributeKey} matches the provided
   * pattern.
   */
  @CanIgnoreReturnValue
  public RuleBasedRoutingSamplerBuilder drop(AttributeKey<String> attributeKey, String pattern) {
    return customize(attributeKey, pattern, Sampler.alwaysOff());
  }

  /**
   * Use the provided sampler when the value of the provided {@link AttributeKey} matches the
   * provided pattern.
   */
  @CanIgnoreReturnValue
  public RuleBasedRoutingSamplerBuilder customize(
      AttributeKey<String> attributeKey, String pattern, Sampler sampler) {
    rules.add(
        new SamplingRule(
            requireNonNull(attributeKey, "attributeKey must not be null"),
            requireNonNull(pattern, "pattern must not be null"),
            requireNonNull(sampler, "sampler must not be null")));
    return this;
  }

  /**
   * Record and sample all spans when the value of the provided {@link AttributeKey} matches the
   * provided pattern.
   */
  @CanIgnoreReturnValue
  public RuleBasedRoutingSamplerBuilder recordAndSample(
      AttributeKey<String> attributeKey, String pattern) {
    return customize(attributeKey, pattern, Sampler.alwaysOn());
  }

  /** Build the sampler based on the rules provided. */
  public RuleBasedRoutingSampler build() {
    return new RuleBasedRoutingSampler(rules, kind, defaultDelegate);
  }
}
