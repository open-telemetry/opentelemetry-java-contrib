/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class EventDataJson {

  @Nullable
  @JsonAttribute(name = "timeUnixNano")
  public String epochNanos;

  @Nullable
  @JsonAttribute(name = "name")
  public String name;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();

  @JsonAttribute(name = "droppedAttributesCount")
  public Integer droppedAttributesCount = 0;
}
