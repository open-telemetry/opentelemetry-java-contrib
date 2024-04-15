/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;

/**
 * A @{@link SpanProcessor} which in addition to all standard operations is capable of modifying and
 * optionally filtering spans in the end-callback.
 *
 * <p>This is done by chaining processors and registering only the first processor with the SDK.
 * Mutations can be performed in {@link #doOnEnd(ReadableSpan)} by wrapping the span in a {@link
 * MutableSpan}
 */
public abstract class AbstractSimpleChainingSpanProcessor implements SpanProcessor {

  protected final SpanProcessor next;
  private final boolean nextRequiresStart;
  private final boolean nextRequiresEnd;

  /**
   * @param next the next processor to be invoked after the one being constructed.
   */
  public AbstractSimpleChainingSpanProcessor(SpanProcessor next) {
    this.next = next;
    nextRequiresStart = next.isStartRequired();
    nextRequiresEnd = next.isEndRequired();
  }

  /**
   * Equivalent of {@link SpanProcessor#onStart(Context, ReadWriteSpan)}. The onStart callback of
   * the next processor must not be invoked from this method, this is already handled by the
   * implementation of {@link #onStart(Context, ReadWriteSpan)}.
   */
  protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {}

  /**
   * Equivalent of {@link SpanProcessor#onEnd(ReadableSpan)}}.
   *
   * <p>If this method returns null, the provided span will be dropped and not passed to the next
   * processor. If anything non-null is returned, the returned instance is passed to the next
   * processor.
   *
   * <p>So in order to mutate the span, simply use {@link MutableSpan#makeMutable(ReadableSpan)} on
   * the provided argument and return the {@link MutableSpan} from this method.
   */
  @CanIgnoreReturnValue
  protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
    return readableSpan;
  }

  /**
   * Indicates if span processor needs to be called on span start
   *
   * @return true, if this implementation would like {@link #doOnStart(Context, ReadWriteSpan)} to
   *     be invoked.
   */
  protected boolean requiresStart() {
    return true;
  }

  /**
   * Indicates if span processor needs to be called on span end
   *
   * @return true, if this implementation would like {@link #doOnEnd(ReadableSpan)} to be invoked.
   */
  protected boolean requiresEnd() {
    return true;
  }

  protected CompletableResultCode doForceFlush() {
    return CompletableResultCode.ofSuccess();
  }

  protected CompletableResultCode doShutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public final void onStart(Context context, ReadWriteSpan readWriteSpan) {
    try {
      if (requiresStart()) {
        doOnStart(context, readWriteSpan);
      }
    } finally {
      if (nextRequiresStart) {
        next.onStart(context, readWriteSpan);
      }
    }
  }

  @Override
  public final void onEnd(ReadableSpan readableSpan) {
    ReadableSpan mappedTo = readableSpan;
    try {
      if (requiresEnd()) {
        mappedTo = doOnEnd(readableSpan);
      }
    } finally {
      if (mappedTo != null && nextRequiresEnd) {
        next.onEnd(mappedTo);
      }
    }
  }

  @Override
  public final boolean isStartRequired() {
    return requiresStart() || nextRequiresStart;
  }

  @Override
  public final boolean isEndRequired() {
    return requiresEnd() || nextRequiresEnd;
  }

  @Override
  public final CompletableResultCode shutdown() {
    return CompletableResultCode.ofAll(Arrays.asList(doShutdown(), next.shutdown()));
  }

  @Override
  public final CompletableResultCode forceFlush() {
    return CompletableResultCode.ofAll(Arrays.asList(doForceFlush(), next.forceFlush()));
  }

  @Override
  public final void close() {
    SpanProcessor.super.close();
  }
}
