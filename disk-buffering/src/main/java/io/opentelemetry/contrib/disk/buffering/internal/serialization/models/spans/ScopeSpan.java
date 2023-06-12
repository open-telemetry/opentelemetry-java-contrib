/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ScopeSignals;
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
