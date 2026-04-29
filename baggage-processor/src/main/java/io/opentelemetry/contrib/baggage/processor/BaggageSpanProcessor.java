/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Collection;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * This span processor copies attributes stored in {@link Baggage} into each newly created {@link
 * io.opentelemetry.api.trace.Span}.
 */
public final class BaggageSpanProcessor implements SpanProcessor {
  private final Predicate<String> baggageKeyPredicate;

  /**
   * @deprecated Use {@link #BaggageSpanProcessor(Collection, Collection)} instead.
   */
  @Deprecated
  public BaggageSpanProcessor(Predicate<String> baggageKeyPredicate) {
    this.baggageKeyPredicate = baggageKeyPredicate;
  }

  /**
   * Creates a new {@link BaggageSpanProcessor} that copies baggage entries with keys that pass the
   * provided include/exclude filtering into the newly created {@link
   * io.opentelemetry.api.trace.Span}, when both arguments are null or empty all baggage are
   * included.
   *
   * @param included list of included attribute patterns to include
   * @param excluded list of excluded attribute patterns to exclude
   */
  public BaggageSpanProcessor(
      @Nullable Collection<String> included, @Nullable Collection<String> excluded) {
    this.baggageKeyPredicate = IncludeExcludePredicate.createPatternMatching(included, excluded);
  }

  /**
   * Creates a new {@link BaggageSpanProcessor} that copies all baggage entries into the newly
   * created span.
   *
   * @return baggage span processor including all attributes
   * @deprecated use {@code new BaggageSpanProcessor(Collections.singletonList("*"), null)}
   *     instead
   */
  @Deprecated
  public static BaggageSpanProcessor allowAllBaggageKeys() {
    return new BaggageSpanProcessor(baggageKey -> true);
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Baggage.fromContext(parentContext)
        .forEach(
            (s, baggageEntry) -> {
              if (baggageKeyPredicate.test(s)) {
                span.setAttribute(s, baggageEntry.getValue());
              }
            });
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public String toString() {
    return "BaggageSpanProcessor{baggageKeyPredicate=" + baggageKeyPredicate + '}';
  }
}
