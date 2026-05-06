/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import java.util.Collection;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * This log record processor copies attributes stored in {@link Baggage} into each newly created log
 * record.
 */
public final class BaggageLogRecordProcessor implements LogRecordProcessor {

  private final Predicate<String> baggageKeyPredicate;

  /**
   * @deprecated Use {@link #BaggageLogRecordProcessor(Collection, Collection)} instead. Most usages
   *     of this method should be replaceable with glob patterns, but not all thus this method is
   *     kept to preserve compatibility.
   */
  @Deprecated
  public BaggageLogRecordProcessor(Predicate<String> baggageKeyPredicate) {
    this.baggageKeyPredicate = baggageKeyPredicate;
  }

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies baggage entries with keys that pass
   * the provided include/exclude filtering into the newly created log record, when both arguments
   * are null or empty all baggage are included.
   *
   * @param included list of included baggage key patterns to include
   * @param excluded list of excluded baggage key patterns to exclude
   */
  public BaggageLogRecordProcessor(
      @Nullable Collection<String> included, @Nullable Collection<String> excluded) {
    this.baggageKeyPredicate = IncludeExcludePredicate.createPatternMatching(included, excluded);
  }

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies all baggage entries into the newly
   * created log record.
   *
   * @return baggage log processor including all attributes
   */
  public static BaggageLogRecordProcessor allowAllBaggageKeys() {
    return new BaggageLogRecordProcessor(null, null);
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Baggage.fromContext(context)
        .forEach(
            (s, baggageEntry) -> {
              if (baggageKeyPredicate.test(s)) {
                logRecord.setAttribute(AttributeKey.stringKey(s), baggageEntry.getValue());
              }
            });
  }

  @Override
  public String toString() {
    return "BaggageLogRecordProcessor{baggageKeyPredicate=" + baggageKeyPredicate + '}';
  }
}
