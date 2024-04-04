/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace.mutable;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.annotation.Nullable;

/**
 * A wrapper around an ended {@link ReadableSpan}, which allows mutation. This is done by wrapping
 * the {@link SpanData} of the provided span and returning a mutated wrapper when {@link
 * #toSpanData()} is called.
 *
 * <p>This class is not thread-safe.Note that after {@link #toSpanData()} has been called, no more
 * mutation are allowed. This guarantees that the returned SpanData is safe to use across threads.
 */
public class MutableSpan implements ReadableSpan {

  private final ReadableSpan delegate;

  @Nullable private MutableSpanData mutableSpanData = null;
  @Nullable private SpanData cachedDelegateSpanData = null;

  private boolean frozen;

  private MutableSpan(ReadableSpan delegate) {
    if (!delegate.hasEnded()) {
      throw new IllegalArgumentException("The provided span has not ended yet!");
    }
    this.delegate = delegate;
  }

  /**
   * If the provided span is already mutable, it is casted and returned. Otherwise, it is wrapped in
   * a new MutableSpan instance and returned.
   *
   * @param span the span to make mutable
   */
  public static MutableSpan makeMutable(ReadableSpan span) {
    if (span instanceof MutableSpan && !((MutableSpan) span).frozen) {
      return (MutableSpan) span;
    } else {
      return new MutableSpan(span);
    }
  }

  public ReadableSpan getOriginalSpan() {
    return delegate;
  }

  private SpanData getDelegateSpanData() {
    if (cachedDelegateSpanData == null) {
      cachedDelegateSpanData = delegate.toSpanData();
    }
    return cachedDelegateSpanData;
  }

  @Override
  public SpanData toSpanData() {
    frozen = true;
    if (mutableSpanData != null) {
      return mutableSpanData;
    }
    return getDelegateSpanData();
  }

  private MutableSpanData mutate() {
    if (frozen) {
      throw new IllegalStateException(
          "toSpanData() has already been called on this span, it is no longer mutable!");
    }
    if (mutableSpanData == null) {
      mutableSpanData = new MutableSpanData(getDelegateSpanData());
    }
    return mutableSpanData;
  }

  @Nullable
  @Override
  public <T> T getAttribute(AttributeKey<T> key) {
    if (mutableSpanData != null) {
      return mutableSpanData.getAttribute(key);
    } else {
      return delegate.getAttribute(key);
    }
  }

  public <T> void removeAttribute(AttributeKey<T> key) {
    mutate().setAttribute(key, null);
  }

  public <T> void setAttribute(AttributeKey<T> key, @Nullable T value) {
    mutate().setAttribute(key, value);
  }

  @Override
  public String getName() {
    if (mutableSpanData != null) {
      return mutableSpanData.getName();
    }
    return delegate.getName();
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    mutate().setName(name);
  }

  @Override
  public SpanContext getSpanContext() {
    return delegate.getSpanContext();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return delegate.getParentSpanContext();
  }

  @Override
  @Deprecated
  public io.opentelemetry.sdk.common.InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return delegate.getInstrumentationLibraryInfo();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return delegate.getInstrumentationScopeInfo();
  }

  @Override
  public boolean hasEnded() {
    return delegate.hasEnded();
  }

  @Override
  public long getLatencyNanos() {
    return delegate.getLatencyNanos();
  }

  @Override
  public SpanKind getKind() {
    return delegate.getKind();
  }
}
