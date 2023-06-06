package io.opentelemetry.contrib.disk.buffer.internal.serialization.logs;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ScopeSignals;
import java.util.ArrayList;
import java.util.List;

public final class ScopeLogs extends ScopeSignals<LogRecordDataJson> {

  @JsonAttribute(name = "logRecords")
  public List<LogRecordDataJson> logRecords = new ArrayList<>();

  @Override
  public void addSignalItem(LogRecordDataJson item) {
    logRecords.add(item);
  }

  @Override
  public List<LogRecordDataJson> getSignalItems() {
    return logRecords;
  }
}
