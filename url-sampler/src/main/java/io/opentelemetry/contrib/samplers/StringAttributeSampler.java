/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.samplers;

import static java.util.stream.Collectors.toMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This sampler drops spans which have one of attribute values matching given pattern.
 */
public class StringAttributeSampler implements Sampler {
  private final Map<AttributeKey<String>, StringMatcher> matchers;
  private final SpanKind kind;
  private final Sampler delegate;

  public StringAttributeSampler(Map<AttributeKey<String>, ? extends Collection<String>> patterns, SpanKind kind, Sampler delegate) {
    this.kind = Objects.requireNonNull(kind);
    this.delegate = Objects.requireNonNull(delegate);
    this.matchers = Objects.requireNonNull(patterns)
        .entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> new StringMatcher(e.getValue())));
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
      return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    for (Map.Entry<AttributeKey<String>, StringMatcher> entry : matchers.entrySet()) {
      String attributeValue = attributes.get(entry.getKey());
      if (attributeValue == null) {
        continue;
      }
      if (entry.getValue().matches(attributeValue)) {
        return SamplingResult.create(SamplingDecision.DROP);
      }
    }
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return toString();
  }

  @Override
  public String toString() {
    return "StringAttributeSampler{" +
           "patterns=" + matchers +
           ", kind=" + kind +
           '}';
  }
}
