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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UrlSampler implements Sampler {
  private final Collection<String> patterns;
  private final Sampler delegate;

  public UrlSampler(Collection<String> patterns, Sampler delegate) {
    this.patterns = Objects.requireNonNull(patterns);
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
    if (patterns.isEmpty()) {
      return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    String urlString = attributes.get(SemanticAttributes.HTTP_URL);
    if (urlString == null) {
      return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    try {
      String path = new URL(urlString).getPath();
      if (patterns.contains(path)) {
        return SamplingResult.create(SamplingDecision.DROP);
      }
    } catch (MalformedURLException e) {
      // No problem, just delegate below
    }
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "UrlSampler{" + patterns + "}";
  }
}
