package io.opentelemetry.contrib.disk.buffer.serialization.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.serialization.common.ScopeSignals;
import java.util.ArrayList;
import java.util.List;

public final class ScopeSpan extends ScopeSignals<SpanDataJson> {

  @JsonAttribute(name = "spans")
  public List<SpanDataJson> spans = new ArrayList<>();

  @Override
  public void addSignalItem(SpanDataJson item) {
    spans.add(item);
  }

  @Override
  public List<SpanDataJson> getSignalItems() {
    return spans;
  }
}
