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
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UrlSampler implements Sampler {
  private final UrlMatcher urlMatcher;
  private final SpanKind kind;
  private final Sampler delegate;

  public UrlSampler(Collection<String> patterns, SpanKind kind, Sampler delegate) {
    this.urlMatcher = new UrlMatcher(Objects.requireNonNull(patterns));
    this.kind = Objects.requireNonNull(kind);
    this.delegate = Objects.requireNonNull(delegate);
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
    String urlString = attributes.get(SemanticAttributes.HTTP_URL);
    if (urlString == null) {
      return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    if (urlMatcher.matches(urlString)) {
      return SamplingResult.create(SamplingDecision.DROP);
    }
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return toString();
  }

  @Override
  public String toString() {
    return "UrlSampler{" +
           "patterns=" + urlMatcher +
           ", kind=" + kind +
           '}';
  }
}
