/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignals;
import java.util.ArrayList;
import java.util.List;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceSpans extends ResourceSignals<ScopeSpan> {

  @JsonAttribute(name = "scopeSpans")
  public List<ScopeSpan> scopeSpans = new ArrayList<>();

  @Override
  public void addScopeSignalsItem(ScopeSpan item) {
    scopeSpans.add(item);
  }

  @Override
  public List<ScopeSpan> getScopeSignals() {
    return scopeSpans;
  }
}
