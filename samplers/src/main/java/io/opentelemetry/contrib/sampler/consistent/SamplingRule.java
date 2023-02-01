/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

class SamplingRule {
  final AttributeKey<String> attributeKey;
  final Sampler delegate;
  final Pattern pattern;

  SamplingRule(AttributeKey<String> attributeKey, String pattern, Sampler delegate) {
    this.attributeKey = attributeKey;
    this.pattern = Pattern.compile(pattern);
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return "SamplingRule{"
        + "attributeKey="
        + attributeKey
        + ", delegate="
        + delegate
        + ", pattern="
        + pattern
        + '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SamplingRule)) {
      return false;
    }
    SamplingRule that = (SamplingRule) o;
    return attributeKey.equals(that.attributeKey) && pattern.equals(that.pattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributeKey, pattern);
  }
}
