/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs;

import com.dslplatform.json.JsonAttribute;
import javax.annotation.Nullable;

public final class BodyJson {

  @Nullable
  @JsonAttribute(name = "stringValue")
  public String stringValue;
}
