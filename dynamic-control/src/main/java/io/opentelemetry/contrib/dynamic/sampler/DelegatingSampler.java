/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class DelegatingSampler implements Sampler {

  private final AtomicReference<Sampler> delegate;

  public DelegatingSampler(Sampler initialDelegate) {
    this.delegate =
        new AtomicReference<>(initialDelegate != null ? initialDelegate : Sampler.alwaysOn());
  }

  public DelegatingSampler() {
    this(Sampler.alwaysOn());
  }

  public void setDelegate(Sampler sampler) {
    delegate.set(sampler != null ? sampler : Sampler.alwaysOn());
  }

  @Override
  public SamplingResult shouldSample(
      Context ctx,
      String traceId,
      String name,
      SpanKind kind,
      Attributes attrs,
      List<LinkData> links) {
    return Objects.requireNonNull(delegate.get())
        .shouldSample(ctx, traceId, name, kind, attrs, links);
  }

  @Override
  public String getDescription() {
    return "delegating/" + Objects.requireNonNull(delegate.get()).getDescription();
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
