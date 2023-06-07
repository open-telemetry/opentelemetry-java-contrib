package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.common.ScopeSignals;
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
