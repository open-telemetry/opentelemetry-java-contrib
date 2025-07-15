/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import java.util.function.Predicate;

/**
 * This log record processor copies attributes stored in {@link Baggage} into each newly created log
 * record.
 */
public class BaggageLogRecordProcessor implements LogRecordProcessor {

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies all baggage entries into the newly
   * created log record.
   */
  public static BaggageLogRecordProcessor allowAllBaggageKeys() {
    return new BaggageLogRecordProcessor(baggageKey -> true, false);
  }

  private final Predicate<String> baggageKeyPredicate;
  private boolean empty;

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies only baggage entries with keys that
   * pass the provided filter into the newly created log record.
   */
  public BaggageLogRecordProcessor(Predicate<String> baggageKeyPredicate) {
    this(baggageKeyPredicate, false); // we don't know if the predicate matches any keys
  }

  BaggageLogRecordProcessor(Predicate<String> baggageKeyPredicate, boolean empty) {
    this.baggageKeyPredicate = baggageKeyPredicate;
    this.empty = empty;
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

  boolean isEmpty() {
    return empty;
  }
}
