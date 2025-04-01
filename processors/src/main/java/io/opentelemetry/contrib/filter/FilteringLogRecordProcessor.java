package io.opentelemetry.contrib.filter;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.function.Predicate;

public class FilteringLogRecordProcessor implements LogRecordProcessor {

  public final LogRecordProcessor delegate;
  public final Predicate<LogRecordData> predicate;

  public FilteringLogRecordProcessor(LogRecordProcessor delegate, Predicate<LogRecordData> predicate) {
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
