/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.function.Predicate;

public final class FilteringLogRecordProcessor implements LogRecordProcessor {

  private final LogRecordProcessor delegate;
  private final Predicate<LogRecordData> predicate;

  public FilteringLogRecordProcessor(
      LogRecordProcessor delegate, Predicate<LogRecordData> predicate) {
    this.delegate = delegate;
    this.predicate = predicate;
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord readWriteLogRecord) {
    if (predicate.test(readWriteLogRecord.toLogRecordData())) {
      delegate.onEmit(context, readWriteLogRecord);
    }
  }
}
