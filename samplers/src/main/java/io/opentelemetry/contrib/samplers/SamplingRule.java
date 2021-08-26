/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import java.util.regex.Pattern;

/** @see RuleBasedRoutingSampler */
class SamplingRule {
  final AttributeKey<String> attributeKey;
  final Sampler delegate;
  final Pattern pattern;

  SamplingRule(AttributeKey<String> attributeKey, String pattern, Sampler delegate) {
    this.attributeKey = Objects.requireNonNull(attributeKey);
    this.pattern = Pattern.compile(Objects.requireNonNull(pattern));
    this.delegate = Objects.requireNonNull(delegate);
  }
}
