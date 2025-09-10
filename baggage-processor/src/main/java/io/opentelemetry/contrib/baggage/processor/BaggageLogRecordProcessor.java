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
public final class BaggageLogRecordProcessor implements LogRecordProcessor {

  private final Predicate<String> baggageKeyPredicate;

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies only baggage entries with keys that
   * pass the provided filter into the newly created log record.
   */
  public BaggageLogRecordProcessor(Predicate<String> baggageKeyPredicate) {
    this.baggageKeyPredicate = baggageKeyPredicate;
  }

  /**
   * Creates a new {@link BaggageLogRecordProcessor} that copies all baggage entries into the newly
   * created log record.
   */
  public static BaggageLogRecordProcessor allowAllBaggageKeys() {
    return new BaggageLogRecordProcessor(baggageKey -> true);
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
}
