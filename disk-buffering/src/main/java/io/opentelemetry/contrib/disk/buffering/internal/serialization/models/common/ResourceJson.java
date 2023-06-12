/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;

public final class ResourceJson {

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();
}
