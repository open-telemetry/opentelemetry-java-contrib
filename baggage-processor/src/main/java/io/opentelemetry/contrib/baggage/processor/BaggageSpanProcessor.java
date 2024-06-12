/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Predicate;

/**
 * This span processor copies attributes stored in {@link Baggage} into each newly created {@link
 * io.opentelemetry.api.trace.Span}.
 */
public class BaggageSpanProcessor implements SpanProcessor {
  private final Predicate<String> baggageKeyPredicate;

  /** use {@link #allowAllBaggageKeys()} instead */
  @Deprecated public static final Predicate<String> allowAllBaggageKeys = baggageKey -> true;

  /**
   * Creates a new {@link BaggageSpanProcessor} that copies only baggage entries with keys that pass
   * the provided filter into the newly created {@link io.opentelemetry.api.trace.Span}.
   */
  public BaggageSpanProcessor(Predicate<String> baggageKeyPredicate) {
    this.baggageKeyPredicate = baggageKeyPredicate;
  }

  /**
   * Creates a new {@link BaggageSpanProcessor} that copies all baggage entries into the newly
   * created {@link io.opentelemetry.api.trace.Span}.
   */
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
}
