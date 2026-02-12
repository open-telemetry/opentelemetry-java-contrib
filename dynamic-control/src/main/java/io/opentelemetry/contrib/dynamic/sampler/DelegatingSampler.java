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
import javax.annotation.Nullable;

/**
 * A {@link Sampler} implementation that delegates sampling decisions to another {@link Sampler}
 * instance held in a volatile field. This allows the effective sampling strategy to be
 * reconfigured at runtime without rebuilding the {@code TracerSdkProvider} or recreating
 * instrumented components.
 *
 * <p>This class is thread-safe. All access to the current delegate sampler is performed through a
 * volatile reference, so sampling decisions and delegate updates may occur concurrently without
 * additional synchronization.
 *
 * <p>The delegate sampler can be updated dynamically via {@link #setDelegate(Sampler)}. Passing
 * {@code null} to {@code setDelegate} or the constructor will cause {@link Sampler#alwaysOn()} to
 * be used as the fallback delegate.
 */
public class DelegatingSampler implements Sampler {

  private volatile Sampler delegate;

  /**
   * Creates a new {@link DelegatingSampler} with the given initial delegate.
   *
   * <p>If {@code initialDelegate} is {@code null}, {@link Sampler#alwaysOn()} will be used as the
   * initial delegate.
   *
   * @param initialDelegate the initial {@link Sampler} to delegate to, or {@code null} to use
   *     {@link Sampler#alwaysOn()} by default
   */
  public DelegatingSampler(@Nullable Sampler initialDelegate) {
    this.delegate = initialDelegate != null ? initialDelegate : Sampler.alwaysOn();
  }

  public DelegatingSampler() {
    this(Sampler.alwaysOn());
  }

  /**
   * Updates the delegate {@link Sampler} used by this {@code DelegatingSampler} at runtime.
   *
   * <p>If {@code sampler} is {@code null}, this method will instead use {@link Sampler#alwaysOn()}
   * as the delegate.
   *
   * @param sampler the new delegate sampler to use, or {@code null} to fall back to {@link
   *     Sampler#alwaysOn()}
   */
  public void setDelegate(@Nullable Sampler sampler) {
    delegate = sampler != null ? sampler : Sampler.alwaysOn();
  }

  @Override
  public SamplingResult shouldSample(
      Context ctx,
      String traceId,
      String name,
      SpanKind kind,
      Attributes attrs,
      List<LinkData> links) {
    return delegate.shouldSample(ctx, traceId, name, kind, attrs, links);
  }

  @Override
  public String getDescription() {
    return "delegating/" + delegate.getDescription();
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
